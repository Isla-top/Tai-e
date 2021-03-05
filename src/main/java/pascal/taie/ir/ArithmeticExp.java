/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.ir;

import pascal.taie.java.types.PrimitiveType;

/**
 * Representation of arithmetic expression, e.g., a + b.
 */
public class ArithmeticExp extends AbstractBinaryExp {

    public enum Op implements BinaryExp.Op {

        /** + */
        ADD("+"),
        /** - */
        SUB("-"),
        /** * */
        MUL("*"),
        /** / */
        DIV("/"),
        /** % */
        REM("%"),
        ;

        private final String name;

        Op(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Op op;

    public ArithmeticExp(Op op, Atom value1, Atom value2) {
        super(value1, value2);
        this.op = op;
    }

    @Override
    protected void validate() {
        assert value1.getType().equals(value2.getType());
        assert value1.getType() instanceof PrimitiveType;
    }

    @Override
    public Op getOperator() {
        return op;
    }

    @Override
    public PrimitiveType getType() {
        return (PrimitiveType) value1.getType();
    }
}
