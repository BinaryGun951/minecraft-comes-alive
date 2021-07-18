package mca.resources.data;

import java.util.Random;

import com.google.common.base.Strings;

import mca.resources.PoolUtil;

public record NameSet (
        String separator,
        String[] first,
        String[] second) {

    public static final NameSet DEFAULT = new NameSet(" ", new String[] {"unknown"}, new String[] {"names"});

    public String toName(Random rng) {
        String first = PoolUtil.pickOne(first(), null, rng);
        String second = PoolUtil.pickOne(second(), null, rng);

        if (Strings.isNullOrEmpty(separator)) {
            return toTitleCase(first + second);
        }

        return toTitleCase(first) + separator + toTitleCase(second);
    }

    static String toTitleCase(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
