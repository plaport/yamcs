package org.yamcs;

import org.yamcs.archive.XtceTmRecorder;
import org.yamcs.time.Instant;
import org.yamcs.utils.TimeEncoding;

/**
 * Packet with acquisition time, generation time and sequence count.
 * 
 * <p>
 * It is assumed that (generation time, sequence count) uniquely identifies the packet
 * 
 * <p>
 * Starting with yamcs 4.11 there is a 32 bitfield status that can be set on the packets by the pre-processor. The bits
 * currently defined:
 * <ul>
 * <li>bit 0 (msb) - always 0</li>
 * <li>bits 1-15 - user defined (pre-processor specific)</li>
 * <li>bits 16-28 - reserved for future Yamcs use</li>
 * <li>bit 29 - DO_NOT_ARCHIVE - the {@link XtceTmRecorder} will not archive the packets that have this bit set.</li>
 * <li>bit 30 - LOCAL_GEN_TIME - when the pre-processor cannot find the generation time, it can use the local time
 * instead and set this bit</li>
 * <li>bit 31(lsb) - INVALID - used for example when the packet fails crc or checksum verification</li>
 * </ul>
 * 
 * <p>
 * For the invalid packets there is an option at the link level to redirect them on a different stream. Using StreamSQL
 * any other packet can be redirected as well.
 * 
 * 
 * @author nm
 *
 */
public class TmPacket {
    final public static int STATUS_MASK_INVALID = 1 << 0;
    final public static int STATUS_MASK_LOCAL_GEN_TIME = 1 << 1;
    final public static int STATUS_MASK_DO_NOT_ARCHIVE = 1 << 2;

    private long rectime = TimeEncoding.INVALID_INSTANT; // Yamcs reception time
    private long gentime = TimeEncoding.INVALID_INSTANT; // generation time
    private Instant ertime = Instant.INVALID_INSTANT; // earth reception time

    private long obt = Long.MIN_VALUE;// on-board time when the time is free running
    private int seqCount;
    private byte[] pkt;
    private int status;

    public TmPacket(long rectime, byte[] pkt) {
        this.rectime = rectime;
        this.pkt = pkt;
    }

    public TmPacket(long rectime, long gentime, int seqCount, byte[] pkt) {
        this.rectime = rectime;
        this.gentime = gentime;
        this.seqCount = seqCount;
        this.pkt = pkt;
    }

    /**
     * The time when the packet has been generated by the payload.
     * 
     * @return
     */
    public long getGenerationTime() {
        return gentime;
    }

    public void setGenerationTime(long time) {
        this.gentime = time;
    }

    /**
     * Return the time when the packet has been received in Yamcs.
     * 
     * @return
     */
    public long getReceptionTime() {
        return rectime;
    }

    /**
     * The sequence count together with the generation time are supposed to uniquely identify the packet.
     * <p>
     * Note that for CCSDS space packets (as per CCSDS 133.0-B), this sequence count is a combination of the application
     * identifier(APID) and packet sequence count.
     * <p>
     * This means that the sequence count cannot be directly used to asses packet loss or packet ordering without having
     * more information about the nature of the count.
     * 
     * @return
     */
    public int getSeqCount() {
        return seqCount;
    }

    public void setSequenceCount(int seqCount) {
        this.seqCount = seqCount;
    }

    public byte[] getPacket() {
        return pkt;
    }

    public int length() {
        return pkt.length;
    }

    public void setInvalid() {
        status |= STATUS_MASK_INVALID;
    }

    public void setInvalid(boolean invalid) {
        status = invalid ? status | STATUS_MASK_INVALID : status & ~STATUS_MASK_INVALID;
    }

    public boolean isInvalid() {
        return (status & STATUS_MASK_INVALID) > 0;
    }

    /**
     * Returns the time when the packet has been received on earth or {@link TimeEncoding#INVALID_INSTANT} if it has not
     * been set.
     * <p>
     * The exact time returned will depend on how the packet has been received. For SLE links it will be the time when
     * the first bit of the last containing frame has been received at the ground station.
     * <p>
     * Some links will not set this parameter.
     * <p>
     * The reason this uses a high resolution instant whereas the others are using millisecond resolution times is that
     * the ground stations usually have high resolution clocks able to provide this accuracy. This is also the time used
     * to perform space to ground time synchronisation.
     * 
     * 
     * @return the time of the reception of the packet on ground.
     */
    public Instant getEarthReceptionTime() {
        return ertime;
    }

    public void setEarthReceptionTime(Instant ertime) {
        this.ertime = ertime;
    }

    /**
     * Use {@link #setEarthReceptionTime(Instant)} instead. (typo fix)
     */
    @Deprecated
    public void setEarthRceptionTime(Instant ertime) {
        setEarthReceptionTime(ertime);
    }

    /**
     * Set the flag that this packet generation time is in fact local time.
     */
    public void setLocalGenTimeFlag() {
        status |= STATUS_MASK_LOCAL_GEN_TIME;
    }

    /**
     * Sets the flag that this packet will not be archived.
     */
    public void setDoNotArchive() {
        status |= STATUS_MASK_DO_NOT_ARCHIVE;
    }

    /**
     * The 32bit flag.
     * <p>
     * Be aware that this will change also the values set by the setInvalid
     * 
     * @return
     */
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getObt() {
        return obt;
    }

    public void setObt(long obt) {
        this.obt = obt;
    }
}
