package org.openflow.protocol.statistics;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * The base class for experimenter implemented statistics message
 * @author David Erickson (daviderickson@cs.stanford.edu)
 * @author Srini Seetharaman (srini.seetharaman@gmail.com)
 */
public class OFExperimenterStatistics implements OFStatistics {
    protected static int MINIMUM_LENGTH = 8;

    protected int experimenter;
    protected int experimenterType;
    protected byte[] experimenterData;

    // non-message fields
    protected int length = MINIMUM_LENGTH;

    /**
     * @return the experimenter
     */
    public int getExperimenter() {
        return experimenter;
    }

    /**
     * @param experimenter the experimenter to set
     */
    public OFExperimenterStatistics setExperimenter(int experimenter) {
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
    public OFExperimenterStatistics setExperimenterType(int experimenterType) {
        this.experimenterType = experimenterType;
        return this;
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
    public OFExperimenterStatistics setExperimenterData(byte[] experimenterData) {
        this.experimenterData = experimenterData;
        if (experimenterData != null)
            this.length = MINIMUM_LENGTH + experimenterData.length;
        return this;
    }

    @Override
    public void readFrom(ByteBuffer data) {
        this.experimenter = data.getInt();
        this.experimenterType = data.getInt();
        if (experimenterData == null)
            experimenterData = new byte[length - MINIMUM_LENGTH];
        data.get(experimenterData);
    }

    @Override
    public void writeTo(ByteBuffer data) {
        data.putInt(this.experimenter);
        data.putInt(this.experimenterType);
        if (experimenterData != null)
            data.put(experimenterData);
    }

    @Override
    public int hashCode() {
        final int prime = 457;
        int result = 1;
        result = prime * result + Arrays.hashCode(experimenterData);
        result = prime * result + experimenter;
        result = prime * result + experimenterType;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OFExperimenterStatistics)) {
            return false;
        }
        OFExperimenterStatistics other = (OFExperimenterStatistics) obj;
        if (!Arrays.equals(experimenterData, other.experimenterData)) {
            return false;
        }
        if (experimenter != other.experimenter) {
            return false;
        }
        if (experimenterType != other.experimenterType) {
            return false;
        }
        return true;
    }

    @Override
    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    @Override
    public int computeLength() {
        return getLength();
    }
}
