package com.github.andrew0030.dakimakuramod.util.iteration;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class LinkedStack<T> implements Iterable<T> {
    Element first;
    Element last;

    public LinkedStack() {
    }

    public void clear() {
        first = last = null;
    }

    public void add(T t) {
        if (first == null) {
            first = new Element(t);
            last = first;
        } else {
            last.next = new Element(t);
            last = last.next;
        }
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return new Iter();
    }

    class Iter implements Iterator<T> {
        Element current = first;

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public T next() {
            Element e = current;
            current = e.next;
            return e.t;
        }
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        for (T t : this) action.accept(t);
    }

    class Element {
        T t;
        Element next;

        public Element(T t) {
            this.t = t;
        }

        public Element(T t, Element next) {
            this.t = t;
            this.next = next;
        }
    }
}
