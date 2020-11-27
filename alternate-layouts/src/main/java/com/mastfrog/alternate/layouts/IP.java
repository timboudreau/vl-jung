/* 
 * Copyright (c) 2020, Tim Boudreau
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.mastfrog.alternate.layouts;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.IntConsumer;

/**
 *
 * @author Tim Boudreau
 */
final class IP implements Comparable<IP>, Iterable<Integer> {

    private static int DEFAULT_SIZE = 12;
    private int[] items;
    private int size;

    private IP(int size, int[] items) {
        this.size = size;
        this.items = Arrays.copyOf(items, size + DEFAULT_SIZE);
    }

    IP(int initialItems) {
        items = new int[initialItems];
    }

    IP() {
        this(DEFAULT_SIZE);
    }

    IP copy() {
        return new IP(size, items);
    }

    public int first() {
        if (size == 0) {
            throw new IndexOutOfBoundsException("Empty");
        }
        return items[0];
    }

    public int last() {
        if (size == 0) {
            throw new IndexOutOfBoundsException("empty");
        }
        return items[size - 1];
    }

    private void growIfNeeded() {
        if (size == items.length - 1) {
            items = Arrays.copyOf(items, items.length + DEFAULT_SIZE);
        }
    }

    public IP addAll(int... values) {
        if (size + values.length < items.length) {
            items = Arrays.copyOf(items, items.length + values.length);
        }
        System.arraycopy(values, 0, items, size, values.length);
        size += values.length;
        return this;
    }

    IP add(int item) {
        growIfNeeded();
        items[size++] = item;
        return this;
    }

    IP append(IP other) {
        int targetSize = size() + other.size();
        if (items.length < targetSize) {
            items = Arrays.copyOf(items, targetSize);
        }
        System.arraycopy(other.items, 0, items, size, other.size());
        size = targetSize;
        return this;
    }

    IP trim() {
        items = Arrays.copyOf(items, size);
        return this;
    }

    public IP childPath() {
        if (size == 0) {
            return this;
        }
        int[] nue = new int[size - 1];
        System.arraycopy(items, 1, nue, 0, nue.length);
        return new IP(nue.length, nue);
    }

    public IP parentPath() {
        if (size == 0) {
            return this;
        }
        int[] nue = new int[size - 1];
        System.arraycopy(items, 0, nue, 0, nue.length);
        return new IP(nue.length, nue);
    }

    IP replace(int index, IP other) {
        size = index;
        append(other);
        return this;
    }

    public IP reversed() {
        int[] nue = new int[size];
        for (int i = 0; i < size; i++) {
            nue[i] = items[size - (i + 1)];
        }
        return new IP(size, nue);
    }

    public int start() {
        return size == 0 ? -1 : items[0];
    }

    public int end() {
        return size == 0 ? -1 : items[size - 1];
    }

    public boolean contains(IP path) {
        if (path.size() > size) {
            return false;
        } else if (path.size() == size) {
            return path.equals(this);
        } else {
            for (int i = 0; i < size - path.size(); i++) {
                if (arraysEquals(items, i, i + path.size(), path.items, 0, path.size())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean arraysEquals(int[] a, int aFromIndex, int aToIndex, int[] b, int bFromIndex, int bToIndex) {
        // JDK 9
//        return Arrays.equals(a, aFromIndex, aToIndex, b, bFromIndex, bToIndex);
        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        for (; aFromIndex < aToIndex && bFromIndex < bToIndex; aFromIndex++, bFromIndex++) {
            if (a[aFromIndex] != b[bFromIndex]) {
                return false;
            }
        }
        return true;
    }

    public int indexOf(int val) {
        for (int i = 0; i < size; i++) {
            if (get(i) == val) {
                return i;
            }
        }
        return -1;
    }

    public boolean contains(int val) {
        for (int i = 0; i < size; i++) {
            if (val == get(i)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public int get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index + " of " + size);
        }
        return items[index];
    }

    public int[] items() {
        return size == items.length ? items : Arrays.copyOf(items, size);
    }

    public void iterate(IntConsumer cons) {
        for (int i = 0; i < size; i++) {
            cons.accept(get(i));
        }
        cons.accept(-1);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof IP) {
            return Arrays.equals(items(), ((IP) o).items());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(items());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < size(); i++) {
            int value = get(i);
            if (sb.length() != 0) {
                sb.append(',');
            }
            sb.append(value);
        }
        return sb.toString();
    }

    @Override
    public int compareTo(IP o) {
        int a = size();
        int b = o.size();
        return a > b ? 1 : a < b ? -1 : 0;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new IIt();
    }

    class IIt implements Iterator<Integer> {

        int pos = -1;

        @Override
        public boolean hasNext() {
            return pos + 1 < size();
        }

        @Override
        public Integer next() {
            return get(++pos);
        }
    }

}
