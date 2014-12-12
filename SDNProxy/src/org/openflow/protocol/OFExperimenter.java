package org.openflow.protocol;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.openflow.util.U16;

/**
 * Represents ofp_experimenter_header
 * @author David Erickson (daviderickson@cs.stanford.edu)
 * @author Srini Seetharaman (srini.seetharaman@gmail.com)
 */
public class OFExperimenter extends OFMessage {
    public static int MINIMUM_LENGTH = 16;

    protected int experimenter;
    protected int experimenterType;
    protected byte[] experimenterData;

    public OFExperimenter() {
        super();
        this.type = OFType.EXPERIMENTER;
        this.length = U16.t(MINIMUM_LENGTH);
    }

    /**
     * @return the experimenter
     */
    public int getExperimenter() {
        return experimenter;
    }

    /**
     * @param experimenter the experimenter to set
     */
    public OFExperimenter setExperimenter(int experimenter) {
        this.experimenter = experimenter;
        return this;
    }

    /**
     * @return the experiment type
     */
    public int getExperimenterType() {
        return experimenterType;
    }

    /**
     * @param experimenterType the experiment type to set
     */
    public OFExperimenter setExperimenterType(int experimenterType) {
        this.experimenterType = experimenterType;
        return this;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        this.experimenter = data.getInt();
        this.experimenterType = data.getInt();
        if (this.length > MINIMUM_LENGTH) {
            this.experimenterData = new byte[this.length - MINIMUM_LENGTH];
            data.get(this.experimenterData);
        }
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.putInt(this.experimenter);
        data.putInt(this.experimenterType);
        if (this.experimenterData != null)
            data.put(this.experimenterData);
    }

    /**
     * @return the experimenterData
     */
    public byte[] getExperimenterData() {
        return experimenterData;
    }

    /**
     * @param experimenterData the experimenterData to set
     */
    public OFExperimenter setExperimenterData(byte[] experimenterData) {
        this.experimenterData = experimenterData;
        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 337;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(experimenterData);
        result = prime * result + experimenter;
        result = prime * result + experimenterType;
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        OFExperimenter other = (OFExperimenter) obj;
        if (!Arrays.equals(experimenterData, other.experimenterData))
            return false;
        if (experimenter != other.experimenter)
            return false;
        if (experimenterType != other.experimenterType)
            return false;
        return true;
    }

    /* (non-Javadoc)
     * @see org.openflow.protocol.OFMessage#computeLength()
     */
    @Override
    public void computeLength() {
        this.length = U16.t(MINIMUM_LENGTH + ((experimenterData != null) ? experimenterData.length : 0));
    }
}
