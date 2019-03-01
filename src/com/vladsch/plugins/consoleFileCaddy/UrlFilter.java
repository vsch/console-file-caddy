/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.execution.filters.BrowserHyperlinkInfo;
import com.intellij.execution.filters.ConsoleFilterProviderEx;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.LazyFileHyperlinkInfo;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.getProperty;
import static java.util.Locale.US;

/**
 * @author yole
 */
public class UrlFilter implements Filter, DumbAware {
    private static final String FILE_URL_PROTOCOL = "file://";
    private final Project myProject;

    public static final Pattern PATTERN_UNIX = Pattern.compile("(?:^|[ \\t:><|/])((?:file://|/(?:[^ \\t:><|/]+/[^ \\t:><|/])+)[^ \\t]+?)((?:[#:(\\[]\\s?L?(\\d+)[^/\\\\\\dL]?(?:L?(\\d+)?[)\\]]?))?)(?:$|[ \\t>)]|:])");
    public static final Pattern PATTERN_WINDOWS = Pattern.compile("(?:^|[ \\t:><|/\\\\])((?:file://|(?:[a-zA-Z]:[/\\\\])(?:[^ \\t:><|/\\\\]+[/\\\\][^ \\t:><|/\\\\])+)[^ \\t]+?)((?:[#:(\\[]\\s?L?(\\d+)[^/\\\\\\dL]?(?:L?(\\d+)?[)\\]]?))?)(?:$|[ \\t>)\\\\]|:])");

    private final Pattern PATTERN;

    public UrlFilter() {
        this(null);
    }

    public UrlFilter(Project project) {
        myProject = project;

        if (getProperty("os.name").toLowerCase(US).startsWith("windows")) {
            PATTERN = PATTERN_WINDOWS;
        } else {
            PATTERN = PATTERN_UNIX;
        }
    }

    @Nullable
    @Override
    public Result applyFilter(@NotNull String line, int entireLength) {
        //if (!URLUtil.canContainUrl(line)) return null;

        int textStartOffset = entireLength - line.length();
        Pattern pattern = PATTERN;
        Matcher m = pattern.matcher(line);
        ResultItem item = null;
        List<ResultItem> items = null;
        while (m.find()) {
            File file = new File(m.group(1));
            if (file.exists()) {
                if (item == null) {
                    item = new ResultItem(textStartOffset + m.start(1), textStartOffset + m.end(2), buildHyperlinkInfo(m.group()));
                } else {
                    if (items == null) {
                        items = new ArrayList<>(2);
                        items.add(item);
                    }
                    items.add(new ResultItem(textStartOffset + m.start(1), textStartOffset + m.end(2), buildHyperlinkInfo(m.group())));
                }
            }
        }

        return items != null ? new Result(items)
                : item != null ? new Result(item.getHighlightStartOffset(), item.getHighlightEndOffset(), item.getHyperlinkInfo())
                : null;
    }

    @NotNull
    protected HyperlinkInfo buildHyperlinkInfo(@NotNull String url) {
        HyperlinkInfo fileHyperlinkInfo = buildFileHyperlinkInfo(url);
        return fileHyperlinkInfo != null ? fileHyperlinkInfo : new BrowserHyperlinkInfo(url);
    }

    @Nullable
    private HyperlinkInfo buildFileHyperlinkInfo(@NotNull String url) {
        if (myProject != null) {
            // vsch: parse ref anchor and convert to line column information if the form is #xxx:yyy, where x & y are digits
            int documentLine = 0;
            int documentColumn = 0;
            Matcher matcher = PATTERN.matcher(url);
            String noAnchorUrl = url;
            if (matcher.matches()) {
                noAnchorUrl = matcher.group(1);
                documentLine = StringUtil.parseInt(matcher.group(3), 0);
                documentColumn = StringUtil.parseInt(matcher.group(4), 0);
                if (documentLine > 0) documentLine--;
                else documentLine = 0;
                if (documentColumn > 0) documentColumn--;
                else documentColumn = 0;

                return new LazyFileHyperlinkInfo(myProject, StringUtil.trimStart(noAnchorUrl, FILE_URL_PROTOCOL), documentLine, documentColumn) {
                    @Nullable
                    @Override
                    public OpenFileDescriptor getDescriptor() {
                        OpenFileDescriptor descriptor = super.getDescriptor();
                        if (descriptor == null) {
                            Messages.showErrorDialog(myProject, "Cannot find file " + StringUtil.trimMiddle(url, 150),
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
            return new Filter[] { new UrlFilter(project) };
        }

        @NotNull
        @Override
        public Filter[] getDefaultFilters(@NotNull Project project) {
            return getDefaultFilters(project, GlobalSearchScope.allScope(project));
        }
    }
}
