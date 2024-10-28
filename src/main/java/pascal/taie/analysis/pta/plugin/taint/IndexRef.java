/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.pta.plugin.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.language.annotation.AnnotationHolder;
import pascal.taie.language.classes.JField;

import java.util.Objects;
import java.util.Set;

record IndexRef(Kind kind, int index, JField field)
        implements Comparable<IndexRef> {

    static final String ARRAY_SUFFIX = "[*]";

    private static final Logger logger = LogManager.getLogger(IndexRef.class);

    static final JField value = Objects.requireNonNull(World.get().getClassHierarchy().getClass("java.lang.String")).getDeclaredField("value");


    enum Kind {
        VAR, ARRAY, FIELD, ARRAY_FIELD, ARRAY_FIELD_FIELD
    }

    @Override
    public JField field() {
        return field;
    }

    @Override
    public int compareTo(IndexRef other) {
        int cmp = index - other.index;
        return cmp != 0 ? cmp : kind.compareTo(other.kind);
    }

    @Override
    public String toString() {
        String base = InvokeUtils.toString(index);
        return switch (kind) {
            case VAR -> base;
            case ARRAY -> base + ARRAY_SUFFIX;
            case FIELD -> base + "." + field.getName();
            case ARRAY_FIELD -> base + "." + field.getName() + ARRAY_SUFFIX;
            case ARRAY_FIELD_FIELD -> base + "." + field.getName() + "." + value.getName() + ARRAY_SUFFIX;
        };
    }

    public JField value(){
        return value;
    }
}
