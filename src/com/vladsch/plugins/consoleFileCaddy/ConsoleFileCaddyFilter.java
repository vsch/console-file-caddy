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

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.DiffManager;
import com.intellij.diff.DiffRequestFactory;
import com.intellij.diff.actions.impl.MutableDiffRequestChain;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.util.DiffUserDataKeys;
import com.intellij.diff.util.Side;
import com.intellij.execution.filters.ConsoleFilterProviderEx;
import com.intellij.execution.filters.FileHyperlinkInfoBase;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.LazyFileHyperlinkInfo;
import com.intellij.execution.filters.LineNumbersMapping;
import com.intellij.ide.actions.QualifiedNameProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWithoutContent;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
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

public class ConsoleFileCaddyFilter implements Filter, DumbAware {
    private static final Logger LOG = getInstance("com.vladsch.plugins.consoleFileCaddy");

    final protected static String FILE_PROTOCOL_PREFIX = "file:///";
    public static final String FQN_PREFIX = "fqn://";
    public static final String DIFF_PREFIX = "diff://";

    final protected static String ANCHOR_SUFFIX = "(?:(?:[#:(\\[]\\s?L?(\\d+)[^/\\\\\\dL]?(?:L?(\\d+)?[)\\]]?))?)(?:$|[ \\t>)]|:])";
    final protected static String ANCHOR_SUFFIX_DIFF_1 = "(?:(?:[#:(\\[]\\s?L?(\\d+)[^/\\\\\\dL]?(?:L?(\\d+)?[)\\]]?))?)";
    final protected static String ANCHOR_SUFFIX_DIFF_2 = "(?:(?:[#:(\\[]\\s?L?(\\d+)[^/\\\\\\dL]?(?:L?(\\d+)?[)\\]]?))?)(?:[ \\t]*?\\&)";
    final protected static String CLASS_FQN = "(?:fqn://[^ \\t]+?)";
    final protected static String CLASS_DIFF = "(?:diff://([^ \\t]+?))";
    final protected static String CLASS_DIFF_HIJACK = "(?:diff://((?:/?\\[)?[^ \\t]+?\\]?))";
    final protected static String DIFF_SECOND = "[ \\t]*?\\?[ \\t]*?([^ \\t]+?)";
    final protected static String DIFF_SECOND_HIJACK = "[ \\t]*?\\?[ \\t]*?((?:/?\\[)?[^ \\t]+?\\]?)";

    final protected static Pattern PATTERN_UNIX = Pattern.compile("(?:^|[ \\t:><|/])"
                    + "((?:(" + CLASS_FQN + "|(?:file://|/(?:[^ \\t:><|/]+/[^ \\t:><|/])+)[^ \\t]+?)" + ANCHOR_SUFFIX + ")"
                    + "|(?:" + CLASS_DIFF + ANCHOR_SUFFIX_DIFF_1 + DIFF_SECOND + ANCHOR_SUFFIX_DIFF_2 + "))"
            , Pattern.MULTILINE);

    final protected static Pattern PATTERN_UNIX_HIJACK = Pattern.compile("(?:^|[ \\t:><|/])"
                    + "((?:(" + CLASS_FQN + "|(?:file://|/(?:[^ \\t:><|/]+/[^ \\t:><|/])+)[^ \\t]+?)" + ANCHOR_SUFFIX + ")"
                    + "|(?:" + CLASS_DIFF_HIJACK + ANCHOR_SUFFIX_DIFF_1 + DIFF_SECOND_HIJACK + ANCHOR_SUFFIX_DIFF_2 + "))"
            , Pattern.MULTILINE);

    final protected static Pattern PATTERN_WINDOWS = Pattern.compile("(?:^|[ \\t:><|/\\\\])"
                    + "((?:(" + CLASS_FQN + "|(?:file://|(?:[a-zA-Z]:[/\\\\])(?:[^ \\t:><|/\\\\]+[/\\\\][^ \\t:><|/\\\\])+)[^ \\t]+?)" + ANCHOR_SUFFIX + ")"
                    + "|(?:" + CLASS_DIFF + ANCHOR_SUFFIX_DIFF_1 + DIFF_SECOND + ANCHOR_SUFFIX_DIFF_2 + "))"
            , Pattern.MULTILINE);

    final protected static Pattern PATTERN_WINDOWS_HIJACK = Pattern.compile("(?:^|[ \\t:><|/\\\\])"
                    + "((?:(" + CLASS_FQN + "|(?:file://|(?:[a-zA-Z]:[/\\\\])(?:[^ \\t:><|/\\\\]+[/\\\\][^ \\t:><|/\\\\])+)[^ \\t]+?)" + ANCHOR_SUFFIX + ")"
                    + "|(?:" + CLASS_DIFF_HIJACK + ANCHOR_SUFFIX_DIFF_1 + DIFF_SECOND_HIJACK + ANCHOR_SUFFIX_DIFF_2 + "))"
            , Pattern.MULTILINE);

    final protected static Pattern USE_PATTERN_UNIX = PATTERN_UNIX_HIJACK;
    final protected static Pattern USE_PATTERN_WINDOWS = PATTERN_WINDOWS_HIJACK;

    private final Project myProject;
    //    private final GlobalSearchScope myScope;
    private final Pattern PATTERN;

    public ConsoleFileCaddyFilter() {
        this(null, null);
    }

    public ConsoleFileCaddyFilter(Project project, GlobalSearchScope scope) {
        myProject = project;
//        myScope = scope;

        if (getProperty("os.name").toLowerCase(US).startsWith("windows")) {
            PATTERN = USE_PATTERN_WINDOWS;
        } else {
            PATTERN = USE_PATTERN_UNIX;
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
            boolean isDiffLink = false;

            HyperlinkInfo hyperlinkInfo = null;
            int leadSlash = PATTERN == PATTERN_UNIX || PATTERN == PATTERN_UNIX_HIJACK ? 1 : 0;

            if (filePath.startsWith(FQN_PREFIX)) {
                if (myProject != null) {
                    // test file link
                    filePath = m.group(2);
                    fixedFilePath = filePath;
                    fixedFilePath = fixedFilePath.substring(FQN_PREFIX.length());
                    LOG.debug("Test file: " + fixedFilePath);
                    hyperlinkInfo = buildFileHyperlinkInfo(true, fixedFilePath, m.group(3), m.group(4));
                }
            } else if (filePath.startsWith(DIFF_PREFIX)) {
                if (myProject != null) {
                    // test file link
                    fixedFilePath = filePath.substring(DIFF_PREFIX.length());
                    LOG.debug("Test file: " + fixedFilePath);
                    String filePath1 = m.group(5);
                    String filePath2 = m.group(8);

                    if (PATTERN == PATTERN_UNIX_HIJACK || PATTERN == PATTERN_WINDOWS_HIJACK) {
                        // strip out [ ] and add leading / if it does not have it
                        filePath1 = getUnHijackedPath(filePath1, leadSlash != 0);
                        filePath2 = getUnHijackedPath(filePath2, leadSlash != 0);
                    }
                    if (filePath1 != null && filePath2 != null) {
                        hyperlinkInfo = buildDiffHyperlinkInfo(filePath1, m.group(6), m.group(7), filePath2, m.group(9), m.group(10));
                    }
                    isDiffLink = true;
                }
            } else {
                filePath = m.group(2);
                fixedFilePath = filePath;
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
                int end = m.end(1);

                int endOffset = textStartOffset + end;
                if (endOffset < startOffset) {
                    continue;
                }

                if (item == null) {
                    item = new ResultItem(startOffset, endOffset, hyperlinkInfo);
                } else {
                    if (items == null) {
                        items = new ArrayList<>(2);
                        items.add(item);
                    }
                    items.add(new ResultItem(startOffset, endOffset, hyperlinkInfo));
                }
            }
        }

        return items != null ? new Result(items)
                : item != null ? new Result(item.getHighlightStartOffset(), item.getHighlightEndOffset(), item.getHyperlinkInfo())
                : null;
    }

    @NotNull
    private static String getUnHijackedPath(String filePath, boolean leadSlash) {
        if (!filePath.isEmpty() && filePath.charAt(0) == '[') filePath = filePath.substring(1);
        else if (filePath.length() > 1 && filePath.charAt(1) == '[') filePath = filePath.charAt(0) + filePath.substring(2);
        // no need can put leading / before the [
        //if (!filePath.isEmpty() && filePath.charAt(0) != (leadSlash ? '/' : '\\')) filePath = (leadSlash ? "/" : "\\") + filePath;
        if (!filePath.isEmpty() && filePath.charAt(filePath.length() - 1) == ']') filePath = filePath.substring(0, filePath.length() - 1);
        return filePath;
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

    @Nullable
    private DiffFileHyperLinkInfo buildDiffHyperlinkInfo(
            @NotNull String filePath1, @Nullable String strFromLine1, @Nullable String strToLine1
            , @NotNull String filePath2, @Nullable String strFromLine2, @Nullable String strToLine2
    ) {
        if (myProject != null) {
            // vsch: parse ref anchor and convert to line column information if the form is #xxx:yyy, where x & y are digits
            int fromLine1 = StringUtil.parseInt(strFromLine1, 0);
            int toLine1 = StringUtil.parseInt(strToLine1, 0);

            if (fromLine1 > 0) fromLine1--;
            else fromLine1 = 0;

            if (toLine1 > 0) toLine1--;
            else toLine1 = 0;

            int fromLine2 = StringUtil.parseInt(strFromLine2, 0);
            int toLine2 = StringUtil.parseInt(strToLine2, 0);

            if (fromLine2 > 0) fromLine2--;
            else fromLine2 = 0;

            if (toLine2 > 0) toLine2--;
            else toLine2 = 0;

            LOG.debug(String.format("Hyperlink info diff paths: %s %d %d %s %d %d", filePath1, fromLine1, toLine1, filePath2, fromLine2, toLine2));

            return new DiffFileHyperLinkInfo(myProject, filePath1, fromLine1, toLine1, filePath2, fromLine2, toLine2) {
                @Nullable
                @Override
                public OpenFileDescriptor getDescriptor(boolean wantSecond) {
                    OpenFileDescriptor descriptor;
                    descriptor = super.getDescriptor(wantSecond);
                    if (descriptor == null) {
                        Messages.showErrorDialog(myProject, "Cannot find diff file " + StringUtil.trimMiddle(wantSecond ? filePath2 : filePath1, 150), "Cannot Open File");
                    }
                    return descriptor;
                }
            };
        }
        return null;
    }

    public static class UrlFilterProvider implements ConsoleFilterProviderEx {
        @NotNull
        @Override
        public Filter[] getDefaultFilters(@NotNull Project project, @NotNull GlobalSearchScope scope) {
            return new Filter[] { new ConsoleFileCaddyFilter(project, scope) };
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

    private static class DiffFileHyperLinkInfo extends FileHyperlinkInfoBase {
        final private Project myProject;
        final private String myPath1;
        final private int myFromLinePath1;
        final private int myToLinePath1;
        final private String myPath2;
        final private int myFromLinePath2;
        final private int myToLinePath2;

        public DiffFileHyperLinkInfo(Project project, String path1, int fromLine1, int toLine1, String path2, int fromLine2, int toLine2) {
            super(project, fromLine1, 0);
            myProject = project;
            myPath1 = path1;
            myFromLinePath1 = fromLine1;
            myToLinePath1 = toLine1;
            myPath2 = path2;
            myFromLinePath2 = fromLine2;
            myToLinePath2 = toLine2;
        }

        @Nullable
        public OpenFileDescriptor getDescriptor(boolean wantSecond) {
            final VirtualFile file = wantSecond ? getVirtualFile2() : getVirtualFile();
            if (file == null || !file.isValid()) return null;

            int line;
            LineNumbersMapping mapping = file.getUserData(LineNumbersMapping.LINE_NUMBERS_MAPPING_KEY);

            int wantedLine = wantSecond ? myFromLinePath2 : myFromLinePath1;
            if (mapping != null) {
                line = mapping.bytecodeToSource(wantedLine + 1) - 1;
                line = line < 0 ? wantedLine : line;
            } else {
                line = wantedLine;
            }

            return new OpenFileDescriptor(myProject, file, line);
        }

        @Nullable
        @Override
        public OpenFileDescriptor getDescriptor() {
            return getDescriptor(false);
        }

        @Nullable
        public OpenFileDescriptor getDescriptor2() {
            return getDescriptor(true);
        }

        @Override
        public void navigate(@NotNull Project project) {
            // open file diff
            OpenFileDescriptor descriptor1 = getDescriptor();
            if (descriptor1 == null) return;
            OpenFileDescriptor descriptor2 = getDescriptor2();
            if (descriptor2 == null) return;

            if (descriptor1.getFile().isDirectory() == descriptor2.getFile().isDirectory()) {
                if (descriptor1.getFile().isDirectory()) {
                    final PsiManager psiManager = PsiManager.getInstance(project);
                    final @Nullable PsiDirectory psiDirectory1 = psiManager.findDirectory(descriptor1.getFile());
                    final @Nullable PsiDirectory psiDirectory2 = psiManager.findDirectory(descriptor1.getFile());
                    if (psiDirectory1 != null && psiManager.isInProject(psiDirectory1)
                            && psiDirectory2 != null && psiManager.isInProject(psiDirectory2)
                    ) {
                        MutableDiffRequestChain chain = createMutableChainFromFiles(project, descriptor1.getFile(), descriptor2.getFile());
                        DiffManager.getInstance().showDiff(project, chain, DiffDialogHints.DEFAULT);
                    }
                } else {
                    // show diff of both files
                    boolean diffShown = false;

                    if (canCompare(descriptor1.getFile(), descriptor2.getFile())) {
                        MutableDiffRequestChain chain = createMutableChainFromFiles(project, descriptor1.getFile(), descriptor2.getFile());

                        chain.putRequestUserData(DiffUserDataKeys.SCROLL_TO_LINE, Pair.create(Side.LEFT, myFromLinePath1));
                        chain.putRequestUserData(DiffUserDataKeys.SCROLL_TO_LINE, Pair.create(Side.RIGHT, myFromLinePath2));
                        DiffManager.getInstance().showDiff(project, chain, DiffDialogHints.DEFAULT);
                        diffShown = true;
                    }

                    if (!diffShown) {
                        // just open files in editor
                        Editor textEditor1 = FileEditorManager.getInstance(project).openTextEditor(descriptor1, false);
                        Editor textEditor2 = FileEditorManager.getInstance(project).openTextEditor(descriptor2, false);
                    }
                }
            }
        }

        protected static boolean hasContent(VirtualFile file) {
            return !(file instanceof VirtualFileWithoutContent);
        }

        private static boolean canCompare(@NotNull VirtualFile file1, @NotNull VirtualFile file2) {
            return !file1.equals(file2) && hasContent(file1) && hasContent(file2);
        }

        @NotNull
        protected static MutableDiffRequestChain createMutableChainFromFiles(
                @Nullable Project project,
                @NotNull VirtualFile file1,
                @NotNull VirtualFile file2
        ) {
            DiffContentFactory contentFactory = DiffContentFactory.getInstance();
            DiffRequestFactory requestFactory = DiffRequestFactory.getInstance();

            DiffContent content1 = contentFactory.create(project, file1);
            DiffContent content2 = contentFactory.create(project, file2);

            MutableDiffRequestChain chain = new MutableDiffRequestChain(content1, content2);

            chain.setWindowTitle(requestFactory.getTitle(file1, file2));
            chain.setTitle1(requestFactory.getContentTitle(file1));
            chain.setTitle2(requestFactory.getContentTitle(file2));

            return chain;
        }

        @Nullable
        protected VirtualFile getVirtualFile(String path) {
            return LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
        }

        @Nullable
        @Override
        protected VirtualFile getVirtualFile() {
            // see if we can convert it from package/file to file
            // first using qualified name providers
            return getVirtualFile(myPath1);
        }

        @Nullable
        protected VirtualFile getVirtualFile2() {
            // see if we can convert it from package/file to file
            // first using qualified name providers
            return getVirtualFile(myPath2);
        }
    }
}
