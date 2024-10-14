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
import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.solver.DefaultSolver;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.core.solver.Transfer;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;

import java.util.function.Supplier;

/**
 * Transfer function that filters out the objects which are
 * not taint object.
 */
public class PrimitiveTaintFilter implements Transfer {

    private static final String TAINT = "TaintObj";
    /**
     * The guard type.
     */
    private final Type type;

    private final TypeSystem typeSystem;

    private final Supplier<PointsToSet> ptsFactory;

    private static final Logger logger = LogManager.getLogger(PrimitiveTaintFilter.class);


    public PrimitiveTaintFilter(Type type, Solver solver) {
        this.type = type;
        this.typeSystem = solver.getTypeSystem();
        this.ptsFactory = solver::makePointsToSet;
    }

    @Override
    public PointsToSet apply(PointerFlowEdge edge, PointsToSet input) {
        PointsToSet result = ptsFactory.get();
        input.objects()
                .filter(o -> o.getObject() instanceof MockObj mockObj &&
                             mockObj.getDescriptor().string().equals(TAINT))
                .forEach(result::addObject);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PrimitiveTaintFilter that)) {
            return false;
        }
        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }
}
