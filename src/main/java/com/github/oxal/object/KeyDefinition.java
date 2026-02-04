package com.github.oxal.object;

import lombok.*;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
public class KeyDefinition {
    String name;
    Class<?> type;

    public boolean sameName(String name) {
        return this.name.equals(name);
    }
}
