package org.openflow.protocol;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.openflow.util.U16;

/**
 * Represents an ofp_error_experimenter_msg
 *
 * @author Srini Seetharaman (srini.seetharaman@gmail.com)
 */
public class OFExperimenterError extends OFError {
    public static int MINIMUM_LENGTH = 16;

    protected short expType;
    protected int experimenter;
    protected byte[] errorData;

    public OFExperimenterError() {
        super();
        this.errorType = (short) OFError.OFErrorType.OFPET_EXPERIMENTER.ordinal();
        this.length = U16.t(MINIMUM_LENGTH);
    }

    /**
     * Get experimenter_id
     * @return
     */
    public int getExperimenter() {
        return this.experimenter;
    }

    /**
     * Set experimenter_id
     * @param experimenter
     */
    public OFExperimenterError setExperimenter(int experimenter) {
        this.experimenter = experimenter;
        return this;
    }

    /**
     * Returns the packet data
     * @return
     */
    public byte[] getPacketData() {
        return this.errorData;
    }

    /**
     * Sets the packet data, and updates the length of this message
     * @param errorData
     */
    public OFExperimenterError setPacketData(byte[] errorData) {
        this.errorData = errorData;
        this.length = U16.t(OFExperimenterError.MINIMUM_LENGTH + errorData.length);
        return this;
    }

    /**
     * Get in_port
     * @return
     */
    public short getExpType() {
        return this.expType;
    }

    /**
     * Set in_port
     * @param expType
     */
    public OFExperimenterError setExpType(short expType) {
        this.expType = expType;
        return this;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        this.expType = data.getShort();
        this.experimenter = data.getInt();
        this.errorData = new byte[getLengthU() - MINIMUM_LENGTH];
        data.get(this.errorData);
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.putShort(expType);
        data.putInt(experimenter);
        data.put(this.errorData);
    }

    @Override
    public int hashCode() {
        final int prime = 283;
        int result = super.hashCode();
        result = prime * result + expType;
        result = prime * result + experimenter;
        result = prime * result + Arrays.hashCode(errorData);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof OFExperimenterError)) {
            return false;
        }
        OFExperimenterError other = (OFExperimenterError) obj;
        if (experimenter != other.experimenter) {
            return false;
        }
        if (expType != other.expType) {
            return false;
        }
        if (!Arrays.equals(errorData, other.errorData)) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.openflow.protocol.OFMessage#computeLength()
     */
    @Override
    public void computeLength() {
        this.length = U16.t(MINIMUM_LENGTH + ((errorData != null) ? errorData.length : 0));
    }
}
