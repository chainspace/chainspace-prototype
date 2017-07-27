package foo.gettingstarted.client;

import bftsmart.tom.core.messages.TOMMessage;

public interface Extractor {
    TOMMessage extractResponse(TOMMessage[] replies, int sameContent, int lastReceived);
}
