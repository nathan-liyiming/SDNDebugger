package org.openflow.protocol;

import java.nio.ByteBuffer;
import java.util.List;

import org.openflow.protocol.factory.OFStatisticsFactory;
import org.openflow.protocol.factory.OFStatisticsFactoryAware;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.util.U16;


/**
 * Base class for multipart messages (primarily statistics requests/replies)
 *
 * @author David Erickson (daviderickson@cs.stanford.edu) - Mar 27, 2010
 */
public abstract class OFStatisticsMessageBase extends OFMessage implements
        OFStatisticsFactoryAware {
    public static int MINIMUM_LENGTH = 16;

    protected OFStatisticsFactory statisticsFactory;
    protected OFStatisticsType statisticsType;
    protected short flags;
    protected List<? extends OFStatistics> statistics;
    
    // if statisticsType == null
    private short statisticsTypeValue;

    /**
     * Construct a ofp_statistics_* message
     */
    public OFStatisticsMessageBase() {
        super();
        this.length = U16.t(MINIMUM_LENGTH);
    }

    /**
     * @return the statisticsType
     */
    public OFStatisticsType getStatisticsType() {
        return statisticsType;
    }

    /**
     * @param statisticsType the statisticsType to set
     */
    public OFStatisticsMessageBase setStatisticsType(OFStatisticsType statisticsType) {
        this.statisticsType = statisticsType;
        return this;
    }

    /**
     * @return the flags
     */
    public short getFlags() {
        return flags;
    }

    /**
     * @param flags the flags to set
     */
    public OFStatisticsMessageBase setFlags(short flags) {
        this.flags = flags;
        return this;
    }

    public OFStatistics getFirstStatistics() {
        if (statistics == null ) {
            throw new RuntimeException("No statistics statistics data available");
        }
        if (statistics.size() == 0) {
            throw new RuntimeException("No statistics statistics data available");
        }
        return statistics.get(0);
    }

    /**
     * @param statistics the statistics to set
     */
    public void setStatistics(List<? extends OFStatistics> statistics) {
        this.statistics = statistics;
    }

    @Override
    public void setStatisticsFactory(OFStatisticsFactory statisticsFactory) {
        this.statisticsFactory = statisticsFactory;
    }

    /**
     * Hacked by brown sys.
     */
    @Override
    public void readFrom(ByteBuffer data) {
        super.readFrom(data);
        short tmpType = data.getShort();
        this.statisticsType = OFStatisticsType.valueOf(tmpType, this
                .getType());
        this.flags = data.getShort();
        data.getInt(); //pad
        if (this.statisticsType == null && this.getType() == OFType.STATS_REQUEST) {
        	if (tmpType == 0 || tmpType == 7 || tmpType == 8 || tmpType == 11 || tmpType == 13) {
        		this.statisticsTypeValue = tmpType;
        	} else {
        		throw new RuntimeException("statisticsType not set");
        	}
        } else {
        	if (this.statisticsFactory == null)
        		throw new RuntimeException("OFStatisticsFactory not set");
        	this.statistics = statisticsFactory.parseStatistics(this.getType(),
        			this.statisticsType, data, super.getLengthU() - MINIMUM_LENGTH);
        }
    }

    /**
     * Hacked by brown sys.
     */
    @Override
    public void writeTo(ByteBuffer data) {
        super.writeTo(data);
        if (this.statisticsType == null) {
        	data.putShort(this.statisticsTypeValue);
        } else {
        	data.putShort(this.statisticsType.getTypeValue());
        }
        data.putShort(this.flags);
        data.putInt(0); //pad
        if (this.statistics != null) {
            for (OFStatistics statistic : this.statistics) {
                statistic.writeTo(data);
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 317;
        int result = super.hashCode();
        result = prime * result + flags;
        result = prime * result
                + ((statisticsType == null) ? 0 : statisticsType.hashCode());
        result = prime * result
                + ((statistics == null) ? 0 : statistics.hashCode());
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
        if (!(obj instanceof OFStatisticsMessageBase)) {
            return false;
        }
        OFStatisticsMessageBase other = (OFStatisticsMessageBase) obj;
        if (flags != other.flags) {
            return false;
        }
        if (statisticsType == null) {
            if (other.statisticsType != null) {
                return false;
            }
        } else if (!statisticsType.equals(other.statisticsType)) {
            return false;
        }
        if (statistics == null) {
            if (other.statistics != null) {
                return false;
            }
        } else if (!statistics.equals(other.statistics)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "OFStatisticsMessage [type=" + statisticsType + ", flags=" + flags + 
                ", data=" + statistics + "]";
    }

    /* (non-Javadoc)
     * @see org.openflow.protocol.OFMessage#computeLength()
     */
    @Override
    public void computeLength() {
        int l = MINIMUM_LENGTH;
        if (statistics != null) {
            for (OFStatistics stat : statistics) {
                l += stat.computeLength();
            }
        }
        this.length = U16.t(l);
    }
}
