package li.cil.oc2.common.serialization.nbt;

import net.minecraft.nbt.*;

final class BooleanArraySerializer implements NBTArraySerializer {
    @Override
    public Tag serialize(final Object value) {
        final boolean[] data = (boolean[]) value;
        final byte[] convertedData = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            convertedData[i] = data[i] ? (byte) 1 : (byte) 0;
        }
        return new ByteArrayTag(convertedData);
    }

    @Override
    public Object deserialize(final Tag tag, final Class<?> type, final Object into) {
        boolean[] data = (boolean[]) into;
        if (tag instanceof final ByteArrayTag byteArrayTag) {
            final byte[] convertedData = byteArrayTag.getAsByteArray();
            if (data == null || data.length != convertedData.length) {
                data = new boolean[convertedData.length];
            }
            for (int i = 0; i < convertedData.length; i++) {
                data[i] = convertedData[i] != 0;
            }
        }
        return data;
    }
}

final class ByteArraySerializer implements NBTArraySerializer {
    @Override
    public Tag serialize(final Object value) {
        return new ByteArrayTag((byte[]) value);
    }

    @Override
    public Object deserialize(final Tag tag, final Class<?> type, final Object into) {
        final byte[] data = (byte[]) into;
        if (tag instanceof final ByteArrayTag byteArrayTag) {
            final byte[] serializedData = byteArrayTag.getAsByteArray();
            if (data == null || data.length != serializedData.length) {
                return serializedData;
            }
            System.arraycopy(serializedData, 0, data, 0, serializedData.length);
        }
        return data;
    }
}

final class CharArraySerializer implements NBTArraySerializer {
    @Override
    public Tag serialize(final Object value) {
        final char[] data = (char[]) value;
        final int[] convertedData = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            convertedData[i] = data[i];
        }
        return new IntArrayTag(convertedData);
    }

    @Override
    public Object deserialize(final Tag tag, final Class<?> type, final Object into) {
        char[] data = (char[]) into;
        if (tag instanceof final IntArrayTag intArrayTag) {
            final int[] convertedData = intArrayTag.getAsIntArray();
            if (data == null || data.length != convertedData.length) {
                data = new char[convertedData.length];
            }
            for (int i = 0; i < convertedData.length; i++) {
                data[i] = (char) convertedData[i];
            }
        }
        return data;
    }
}

final class ShortArraySerializer implements NBTArraySerializer {
    @Override
    public Tag serialize(final Object value) {
        final short[] data = (short[]) value;
        final int[] convertedData = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            convertedData[i] = data[i];
        }
        return new IntArrayTag(convertedData);
    }

    @Override
    public Object deserialize(final Tag tag, final Class<?> type, final Object into) {
        short[] data = (short[]) into;
        if (tag instanceof final IntArrayTag intArrayTag) {
            final int[] convertedData = intArrayTag.getAsIntArray();
            if (data == null || data.length != convertedData.length) {
                data = new short[convertedData.length];
            }
            for (int i = 0; i < convertedData.length; i++) {
                data[i] = (short) convertedData[i];
            }
        }
        return data;
    }
}

final class IntArraySerializer implements NBTArraySerializer {
    @Override
    public Tag serialize(final Object value) {
        return new IntArrayTag((int[]) value);
    }

    @Override
    public Object deserialize(final Tag tag, final Class<?> type, final Object into) {
        final int[] data = (int[]) into;
        if (tag instanceof final IntArrayTag intArrayTag) {
            final int[] serializedData = intArrayTag.getAsIntArray();
            if (data == null || data.length != serializedData.length) {
                return serializedData;
            }
            System.arraycopy(serializedData, 0, data, 0, serializedData.length);
        }
        return data;
    }
}

final class LongArraySerializer implements NBTArraySerializer {
    @Override
    public Tag serialize(final Object value) {
        return new LongArrayTag((long[]) value);
    }

    @Override
    public Object deserialize(final Tag tag, final Class<?> type, final Object into) {
        final long[] data = (long[]) into;
        if (tag instanceof final LongArrayTag longArrayTag) {
            final long[] serializedData = longArrayTag.getAsLongArray();
            if (data == null || data.length != serializedData.length) {
                return serializedData;
            }
            System.arraycopy(serializedData, 0, data, 0, serializedData.length);
        }
        return data;
    }
}

final class FloatArraySerializer implements NBTArraySerializer {
    @Override
    public Tag serialize(final Object value) {
        final float[] data = (float[]) value;
        final int[] convertedData = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            convertedData[i] = Float.floatToRawIntBits(data[i]);
        }
        return new IntArrayTag(convertedData);
    }

    @Override
    public Object deserialize(final Tag tag, final Class<?> type, final Object into) {
        float[] data = (float[]) into;
        if (tag instanceof final IntArrayTag intArrayTag) {
            final int[] convertedData = intArrayTag.getAsIntArray();
            if (data == null || data.length != convertedData.length) {
                data = new float[convertedData.length];
            }
            for (int i = 0; i < convertedData.length; i++) {
                data[i] = Float.intBitsToFloat(convertedData[i]);
            }
        }
        return data;
    }
}

final class DoubleArraySerializer implements NBTArraySerializer {
    @Override
    public Tag serialize(final Object value) {
        final double[] data = (double[]) value;
        final long[] convertedData = new long[data.length];
        for (int i = 0; i < data.length; i++) {
            convertedData[i] = Double.doubleToRawLongBits(data[i]);
        }
        return new LongArrayTag(convertedData);
    }

    @Override
    public Object deserialize(final Tag tag, final Class<?> type, final Object into) {
        double[] data = (double[]) into;
        if (tag instanceof final LongArrayTag longArrayTag) {
            final long[] convertedData = longArrayTag.getAsLongArray();
            if (data == null || data.length != convertedData.length) {
                data = new double[convertedData.length];
            }
            for (int i = 0; i < convertedData.length; i++) {
                data[i] = Double.longBitsToDouble(convertedData[i]);
            }
        }
        return data;
    }
}
