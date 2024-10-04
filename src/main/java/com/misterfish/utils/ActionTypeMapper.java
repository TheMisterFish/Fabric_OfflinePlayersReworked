package com.misterfish.utils;

import com.misterfish.helper.EntityPlayerActionPack;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

public class ActionTypeMapper {

    private static final List<Map.Entry<String, EntityPlayerActionPack.ActionType>> ACTION_TYPE_MAPPINGS = List.of(
            new AbstractMap.SimpleEntry<>("attack", EntityPlayerActionPack.ActionType.ATTACK),
            new AbstractMap.SimpleEntry<>("break", EntityPlayerActionPack.ActionType.ATTACK),
            new AbstractMap.SimpleEntry<>("place", EntityPlayerActionPack.ActionType.USE),
            new AbstractMap.SimpleEntry<>("use", EntityPlayerActionPack.ActionType.USE),
            new AbstractMap.SimpleEntry<>("crouch", EntityPlayerActionPack.ActionType.CROUCH),
            new AbstractMap.SimpleEntry<>("jump", EntityPlayerActionPack.ActionType.JUMP),
            new AbstractMap.SimpleEntry<>("eat", EntityPlayerActionPack.ActionType.EAT),
            new AbstractMap.SimpleEntry<>("drop_item", EntityPlayerActionPack.ActionType.DROP_ITEM),
            new AbstractMap.SimpleEntry<>("drop_stack", EntityPlayerActionPack.ActionType.DROP_STACK),
            new AbstractMap.SimpleEntry<>("move_forward", EntityPlayerActionPack.ActionType.MOVE_FORWARD),
            new AbstractMap.SimpleEntry<>("move_forwards", EntityPlayerActionPack.ActionType.MOVE_FORWARD),
            new AbstractMap.SimpleEntry<>("move_backward", EntityPlayerActionPack.ActionType.MOVE_BACKWARD),
            new AbstractMap.SimpleEntry<>("move_backwards", EntityPlayerActionPack.ActionType.MOVE_BACKWARD),
            new AbstractMap.SimpleEntry<>("disconnect", EntityPlayerActionPack.ActionType.DISCONNECT)
    );

    public static EntityPlayerActionPack.ActionType getActionType(String action) {
        return ACTION_TYPE_MAPPINGS.stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(action))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}