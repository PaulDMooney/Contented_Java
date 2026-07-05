package com.contented.contented.contentitem.model;

/**
 * The lifecycle state of a single content-item version. At most one {@link #WORKING} and one
 * {@link #LIVE} version may exist per identifier; {@link #ARCHIVED} versions are retained history
 * and uncapped. Working is the only mutable state; live and archived versions are immutable.
 */
public enum ContentItemState {
    WORKING,
    LIVE,
    ARCHIVED
}
