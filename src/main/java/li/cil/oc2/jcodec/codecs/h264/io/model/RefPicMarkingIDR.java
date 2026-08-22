/* SPDX-License-Identifier: BSD 2-Clause "Simplified" License */

// Vendored from JCodec (https://github.com/jcodec/jcodec), version 0.2.5, org.jcodec:jcodec

package li.cil.oc2.jcodec.codecs.h264.io.model;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Reference picture marking used for IDR frames
 *
 * @author The JCodec project
 */
public record RefPicMarkingIDR(boolean discardDecodedPics, boolean useForlongTerm) { }
