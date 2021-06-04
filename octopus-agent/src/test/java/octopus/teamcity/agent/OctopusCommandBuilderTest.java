package octopus.teamcity.agent;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OctopusCommandBuilderTest {

    private void checkValues(final String input, final List<String> outputs) {
        assertThat(OctopusCommandBuilder.splitSpaceSeparatedValues(input)).containsAll(outputs);
    }

    @Test
    public void stringsCorrectlySeparateOnSpaceDelimiter() {
        checkValues(null, Collections.emptyList());
        checkValues("", Collections.emptyList());
        checkValues("    ", Collections.emptyList());
        checkValues("\t", Collections.emptyList());
        checkValues("\n", Collections.emptyList());
        checkValues("\r", Collections.emptyList());
        checkValues("\r\n", Collections.emptyList());
        checkValues("\t\r\n", Collections.emptyList());
        checkValues("a b c", Lists.newArrayList("a", "b", "c"));
        checkValues("a  b c", Lists.newArrayList("a", "b", "c"));
        checkValues("a  b  c", Lists.newArrayList("a", "b", "c"));
        checkValues("a\tb\tc", Lists.newArrayList("a", "b", "c"));

        checkValues("aa bb cc", Lists.newArrayList("aa", "bb", "cc"));

        checkValues(" a  b  c", Lists.newArrayList(  " a", "b", "c")); // IS THIS EXPECTED BEHAVIOUR?
        checkValues("\ta  b  c", Lists.newArrayList(  "\ta", "b", "c")); // IS THIS EXPECTED BEHAVIOUR?
    }

    @Test
    public void itemsStartingWithDashHaveEndEqualsStrippedAsRequiredWhenSpaceDelimited() {
        checkValues("-option=value", Lists.newArrayList("-option", "value"));
        checkValues("-option value", Lists.newArrayList("-option", "value"));
        checkValues("-option=value parameter", Lists.newArrayList("-option", "value", "parameter"));
        checkValues("-option -option2=value", Lists.newArrayList("-option", "-option2", "value"));
    }

    @Test
    public void quoteMarksKeepThingsTogetherWhenSpaceDelimited() {
        checkValues("-option=\"value string\"", Lists.newArrayList("-option", "value string"));
        checkValues("-option=\"value=string\"", Lists.newArrayList("-option", "value=string"));
        checkValues("-option=\"value=string\"", Lists.newArrayList("-option", "value=string"));
    }

    // Comma Delimited Tests from here down
    private void checkCommaSeparatedValues(final String input, final List<String> outputs) {
        assertThat(OctopusCommandBuilder.splitCommaSeparatedValues(input)).containsAll(outputs);
    }

    @Test
    public void commaDelimitedSplitsOnlyOnCommas() {
        checkCommaSeparatedValues("a,b,c", Lists.newArrayList("a", "b", "c"));
        checkCommaSeparatedValues(" a , b , c ", Lists.newArrayList("a", "b", "c"));
    }

    @Test
    public void commaDelimitedProtectsCommasInStrings() {
        checkCommaSeparatedValues("\"a,b\",c", Lists.newArrayList("\"a,b\"", "c"));
        checkCommaSeparatedValues("\"a,b,c", Lists.newArrayList("\"a","b","c"));
        checkCommaSeparatedValues("a,b\",c\"", Lists.newArrayList("a","b\",c\"")); //IS THIS EXPECTED BEHAVIOUR
    }
}