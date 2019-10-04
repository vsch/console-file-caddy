/*
 * Copyright 2000-2015 JetBrains s.r.o.
 * Copyright 2019 Vladimir Schneider, vladimir.schneider@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vladsch.plugins.consoleFileCaddy;

import com.intellij.execution.filters.ConsoleFilterProviderEx;
import com.intellij.execution.filters.FileHyperlinkInfoBase;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.LazyFileHyperlinkInfo;
import com.intellij.ide.actions.QualifiedNameProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.openapi.diagnostic.Logger.getInstance;
import static java.lang.System.getProperty;
import static java.util.Locale.US;

public class UrlFilter implements Filter, DumbAware {
    private static final Logger LOG = getInstance("com.vladsch.plugins.consoleFileCaddy");

    final protected static String TEST_PROTOCOL_PREFIX = "fqn://";
    final protected static String FILE_PROTOCOL_PREFIX = "file:///";
    final protected static String ANCHOR_SUFFIX = "((?:[#:(\\[]\\s?L?(\\d+)[^/\\\\\\dL]?(?:L?(\\d+)?[)\\]]?))?)(?:$|[ \\t>)]|:])";
    final protected static String TEST_CLASS_FQN = "(?:" + TEST_PROTOCOL_PREFIX + "[^ \\t]+?)";
    public static final Pattern PATTERN_UNIX = Pattern.compile("(?:^|[ \\t:><|/])" +
            "(" + TEST_CLASS_FQN
            + "|(?:file://|/(?:[^ \\t:><|/]+/[^ \\t:><|/])+)[^ \\t]+?)"
            + ANCHOR_SUFFIX, Pattern.MULTILINE);
    public static final Pattern PATTERN_WINDOWS = Pattern.compile("(?:^|[ \\t:><|/\\\\])" +
            "(" + TEST_CLASS_FQN
            + "|(?:file://|(?:[a-zA-Z]:[/\\\\])(?:[^ \\t:><|/\\\\]+[/\\\\][^ \\t:><|/\\\\])+)[^ \\t]+?" +
            ")"
            + ANCHOR_SUFFIX, Pattern.MULTILINE);

    private final Project myProject;
    //    private final GlobalSearchScope myScope;
    private final Pattern PATTERN;

    public UrlFilter() {
        this(null, null);
    }

    public UrlFilter(Project project, GlobalSearchScope scope) {
        myProject = project;
//        myScope = scope;

        if (getProperty("os.name").toLowerCase(US).startsWith("windows")) {
            PATTERN = PATTERN_WINDOWS;
        } else {
            PATTERN = PATTERN_UNIX;
        }
    }

    @Nullable
    @Override
    public Result applyFilter(@NotNull String line, int entireLength) {
        int textStartOffset = entireLength - line.length();
        Matcher m = PATTERN.matcher(line);
        ResultItem item = null;
        List<ResultItem> items = null;
        while (m.find()) {
            String filePath = m.group(1);
            String fixedFilePath = filePath;
            LOG.debug("File path: " + filePath);

            HyperlinkInfo hyperlinkInfo = null;
            if (filePath.startsWith(TEST_PROTOCOL_PREFIX)) {
                if (myProject != null) {
                    // test file link
                    fixedFilePath = filePath.substring(TEST_PROTOCOL_PREFIX.length());
                    LOG.debug("Test file: " + fixedFilePath);
                    hyperlinkInfo = buildFileHyperlinkInfo(true, fixedFilePath, m.group(3), m.group(4));
                }
            } else {
                int leadSlash = PATTERN == PATTERN_UNIX ? 1 : 0;
                if (filePath.startsWith(FILE_PROTOCOL_PREFIX)) fixedFilePath = filePath.substring(FILE_PROTOCOL_PREFIX.length() - leadSlash);
                else if (filePath.startsWith(FILE_PROTOCOL_PREFIX.substring(0, FILE_PROTOCOL_PREFIX.length() - 1))) fixedFilePath = filePath.substring(FILE_PROTOCOL_PREFIX.length() - 1 - leadSlash);
                else if (filePath.startsWith(FILE_PROTOCOL_PREFIX.substring(0, FILE_PROTOCOL_PREFIX.length() - 2))) fixedFilePath = filePath.substring(FILE_PROTOCOL_PREFIX.length() - 2 - leadSlash);
                LOG.debug("Fixed file path: " + fixedFilePath);

                File file = new File(fixedFilePath);
                if (file.exists()) {
                    hyperlinkInfo = buildFileHyperlinkInfo(false, fixedFilePath, m.group(3), m.group(4));
                }
            }

            if (hyperlinkInfo != null) {
                int startOffset = textStartOffset + m.start(1); // + (filePath.length() + fixedFilePath.length());

                if (item == null) {
                    item = new ResultItem(startOffset, textStartOffset + m.end(2), hyperlinkInfo);
                } else {
                    if (items == null) {
                        items = new ArrayList<>(2);
                        items.add(item);
                    }
                    items.add(new ResultItem(startOffset, textStartOffset + m.end(2), hyperlinkInfo));
                }
            }
        }

        return items != null ? new Result(items)
                : item != null ? new Result(item.getHighlightStartOffset(), item.getHighlightEndOffset(), item.getHyperlinkInfo())
                : null;
    }

    @Nullable
    private HyperlinkInfo buildFileHyperlinkInfo(boolean isTestFile, @NotNull String filePath, @Nullable String docLine, @Nullable String docColumn) {
        if (myProject != null) {
            // vsch: parse ref anchor and convert to line column information if the form is #xxx:yyy, where x & y are digits
            int documentLine = StringUtil.parseInt(docLine, 0);
            int documentColumn = StringUtil.parseInt(docColumn, 0);

            if (documentLine > 0) documentLine--;
            else documentLine = 0;

            if (documentColumn > 0) documentColumn--;
            else documentColumn = 0;

            LOG.debug(String.format("Hyperlink info file path: %s %d %d", filePath, documentLine, documentColumn));

            if (isTestFile) {
                return new TestFileHyperLinkInfo(myProject, filePath, documentLine, documentColumn) {
                    @Nullable
                    @Override
                    public OpenFileDescriptor getDescriptor() {
                        OpenFileDescriptor descriptor = null;
                        if (DumbService.getInstance(myProject).isDumb()) {
                            Messages.showErrorDialog(myProject, "Cannot navigate to test file " + StringUtil.trimMiddle(filePath, 150) + "\n until indexing is complete.",
                                    "Cannot Navigate During Indexing");
                        } else {
                            descriptor = super.getDescriptor();
                            if (descriptor == null) {
                                Messages.showErrorDialog(myProject, "Cannot find test file " + StringUtil.trimMiddle(filePath, 150),
                                        "Cannot Open File");
                            }
                        }
                        return descriptor;
                    }
                };
            } else {
                return new LazyFileHyperlinkInfo(myProject, filePath, documentLine, documentColumn) {
                    @Nullable
                    @Override
                    public OpenFileDescriptor getDescriptor() {
                        OpenFileDescriptor descriptor = super.getDescriptor();
                        if (descriptor == null) {
                            Messages.showErrorDialog(myProject, "Cannot find file " + StringUtil.trimMiddle(filePath, 150),
                                    "Cannot Open File");
                        }
                        return descriptor;
                    }
                };
            }
        }
        return null;
    }

    public static class UrlFilterProvider implements ConsoleFilterProviderEx {
        @NotNull
        @Override
        public Filter[] getDefaultFilters(@NotNull Project project, @NotNull GlobalSearchScope scope) {
            return new Filter[] { new UrlFilter(project, scope) };
        }

        @NotNull
        @Override
        public Filter[] getDefaultFilters(@NotNull Project project) {
            return getDefaultFilters(project, GlobalSearchScope.allScope(project));
        }
    }

    private static class TestFileHyperLinkInfo extends FileHyperlinkInfoBase {
        final private Project myProject;
        final private String myPath;

        public TestFileHyperLinkInfo(Project project, String path, int line, int column) {
            super(project, line, column);
            myProject = project;
            myPath = path;
        }

        @Nullable
        @Override
        protected VirtualFile getVirtualFile() {
            // see if we can convert it from package/file to file
            // first using qualified name providers
            if (!DumbService.getInstance(myProject).isDumb()) {
                String useFilePath = myPath;
                for (QualifiedNameProvider provider : QualifiedNameProvider.EP_NAME.getExtensions()) {
                    PsiElement element = null;
                    try {
                        element = provider.qualifiedNameToElement(useFilePath, myProject);
                    } catch (Throwable throwable) {
                        LOG.info(throwable);
                    }

                    if (element == null && "PhpQualifiedNameProvider".startsWith(provider.getClass().getSimpleName())) {
                        // remove :: and the rest
                        int pos = useFilePath.indexOf(':');
                        if (pos > 0) {
                            useFilePath = useFilePath.substring(0, pos);
                            element = provider.qualifiedNameToElement(myPath, myProject);
                        }
                    }

                    if (element != null) {
                        String result = provider.getQualifiedName(element);
                        if (result != null && result.equals(myPath)) {
                            if (!(element instanceof PsiFile) && element.getContainingFile() != null) {
                                final PsiFile psiFile = element.getContainingFile().getOriginalFile(); // use original file, diagnostic/1102
                                return psiFile.getVirtualFile();
                            }
                        }
                    }
                }
            }
            return null;
        }
    }
}
