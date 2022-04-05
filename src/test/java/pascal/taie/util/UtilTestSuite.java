/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.util;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import pascal.taie.util.collection.CollectionTestSuite;
import pascal.taie.util.graph.GraphTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        CollectionTestSuite.class,
        GraphTest.class,
        IndexerTest.class,
})
public class UtilTestSuite {
}
