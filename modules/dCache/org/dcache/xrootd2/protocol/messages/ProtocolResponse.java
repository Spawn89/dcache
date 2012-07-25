package org.dcache.xrootd2.protocol.messages;

import org.dcache.xrootd2.protocol.XrootdProtocol;

public class ProtocolResponse extends AbstractResponseMessage
{
    public ProtocolResponse(int sId, int flags)
    {
        super(sId, XrootdProtocol.kXR_ok, 8);
        putSignedInt(XrootdProtocol.PROTOCOL_VERSION);
        putSignedInt(flags);
    }
}