/*
 * Copyright 2000-2012 Octopus Deploy Pty. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package octopus.teamcity.agent;

import jetbrains.buildServer.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class OctopusCommandBuilder {
    public String[] buildCommand() {
        return buildCommand(false);
    }

    public String[] buildMaskedCommand() {
        return buildCommand(true);
    }

    protected abstract String[] buildCommand(boolean masked);

    protected String Quote(String value) {
        return "\"" + value + "\"";
    }

    public static List<String> splitSpaceSeparatedValues(String text) {
        List<String> results = new ArrayList<String>();
        if (text == null || StringUtil.isEmptyOrSpaces(text)) {
            return results;
        }

        Matcher m = Pattern.compile("(-[\\w-]*=?|[^\"]\\S*|\".+?\")\\s*").matcher(text);
        while (m.find()) {
            String item = m.group(1).replace("\"", "");
            if(item.startsWith("-") && item.endsWith("="))
                item = item.substring(0, item.length() - 1);

            if (item != null && !item.isEmpty()) {
                results.add(item);
            }
        }

        return results;
    }

    public static List<String> splitCommaSeparatedValues(String text) {
        List<String> results = new ArrayList<String>();
        if (text == null || StringUtil.isEmptyOrSpaces(text)) {
            return results;
        }

        String line = text;
        String[] tokens = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

        for (String t : tokens) {
            String trimmed = t.trim();
            if (trimmed.length() > 0) {
                results.add(trimmed);
            }
        }

        return results;
    }
}
