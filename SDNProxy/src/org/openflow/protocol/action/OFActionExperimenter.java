package org.openflow.protocol.action;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 * @author James Hongyi Zeng (hyzeng@stanford.edu)
 */
public class OFActionExperimenter extends OFAction {
    public static int MINIMUM_LENGTH = 8;

    protected int experimenter;
    protected byte[] experimenterData;

    public OFActionExperimenter() {
        super();
        super.setType(OFActionType.EXPERIMENTER);
        super.setLength((short) MINIMUM_LENGTH);
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
    public OFActionExperimenter setExperimenter(int experimenter) {
        this.experimenter = experimenter;
        return this;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        this.experimenter = data.getInt();
        int dataLength = this.length - MINIMUM_LENGTH;
        this.experimenterData = new byte[dataLength];
        data.get(this.experimenterData, 0, dataLength);
    }

    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        data.putInt(this.experimenter);
        data.put(this.experimenterData);
    }

    @Override
    public int hashCode() {
        final int prime = 379;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(experimenterData);
        result = prime * result + experimenter;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof OFActionExperimenter))
            return false;
        OFActionExperimenter other = (OFActionExperimenter) obj;
        if (!Arrays.equals(experimenterData, other.experimenterData))
            return false;
        if (experimenter != other.experimenter)
            return false;
        return true;
    }

    /**
     * @return the experimenterData
     */
    public byte[] getExperimenterData() {
        return experimenterData;
    }

    /**
     * @param experimenterData the experimenter data to set
     */
    public void setExperimenterData(byte[] experimenterData) {
        this.experimenterData = experimenterData;
    }
}
