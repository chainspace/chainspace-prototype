package uk.ac.ucl.cs.sec.chainspace.bft;

import bftsmart.tom.core.messages.TOMMessage;

public interface Extractor {
    TOMMessage extractResponse(TOMMessage[] replies, int sameContent, int lastReceived);
}
