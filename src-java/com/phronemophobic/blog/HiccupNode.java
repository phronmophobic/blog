
package com.phronemophobic.blog;

// package blog;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.ast.Node;

public class HiccupNode extends Node {

    public final Object hiccup;

    @Override
    public boolean hasChildren(){
        return false;
    }


    @Override
    public BasedSequence[] getSegments() {
        return EMPTY_SEGMENTS;
    }

    public HiccupNode(Object hiccup) {
        this.hiccup = hiccup;
    }


}
