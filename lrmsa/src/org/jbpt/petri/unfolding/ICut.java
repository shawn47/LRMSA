package org.jbpt.petri.unfolding;

import org.jbpt.petri.*;

public interface ICut<BPN extends IBPNode<N>, C extends ICondition<BPN, C, E, F, N, P, T, M>, E extends IEvent<BPN, C, E, F, N, P, T, M>, F extends IFlow<N>, N extends INode, P extends IPlace, T extends ITransition, M extends IMarking<F, N, P, T>>
        extends ICoSet<BPN, C, E, F, N, P, T, M> {
}
