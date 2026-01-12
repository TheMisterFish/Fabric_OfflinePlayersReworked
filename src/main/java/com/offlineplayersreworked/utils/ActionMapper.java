package com.offlineplayersreworked.utils;

import com.offlineplayersreworked.offline_config.ModConfigs;
import com.offlineplayersreworked.exception.InvalidActionException;
import com.offlineplayersreworked.exception.InvalidIntervalException;
import com.offlineplayersreworked.exception.InvalidOffsetException;
import com.offlineplayersreworked.exception.UnavailableActionException;
import com.offlineplayersreworked.helper.EntityPlayerActionPack;
import it.unimi.dsi.fastutil.Pair;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

public class ActionMapper {
    public static EntityPlayerActionPack.ActionType getActionType(String action) {
        return ACTION_TYPE_MAPPINGS.stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(action))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    public static ArrayList<Pair<EntityPlayerActionPack.ActionType, EntityPlayerActionPack.Action>> getActionPackList(String[] pairs) {
        ArrayList<Pair<EntityPlayerActionPack.ActionType, EntityPlayerActionPack.Action>> actionList = new ArrayList<>();

        IntStream.range(0, pairs.length).forEach(index -> {
            String pair = pairs[index];
            String[] actionInterval = pair.split(":");
            if (actionInterval.length != 1 && actionInterval.length != 2 && actionInterval.length != 3) {
                throw new InvalidActionException("Invalid format. Use action, action:interval or action:interval:offset.");
            }

            String action = actionInterval[0];

            ModConfigs.AVAILABLE_OPTIONS.stream()
                    .filter(option -> option.equals(action) && getActionType(option) != null)
                    .findAny()
                    .orElseThrow(() -> new UnavailableActionException("Invalid action: " + action + " Is not a valid action."));

            int interval = Objects.equals(action, "break") ? 0 : 20; // by default break should be continuously

            if (actionInterval.length > 1) {
                try {
                    interval = TimeParser.parse(actionInterval[1]);
                } catch(IllegalArgumentException e) {
                    throw new InvalidIntervalException("Invalid interval format: " + e.getMessage());
                }
            }

            Pair<EntityPlayerActionPack.ActionType, EntityPlayerActionPack.Action> actionPair;

            if (actionInterval.length > 2) {
                try {
                    int offset = TimeParser.parse(actionInterval[2]);
                    actionPair = EntityPlayerActionPack.getActionPair(action, interval, offset);
                } catch(IllegalArgumentException e) {
                    throw new InvalidOffsetException("Invalid offset format: " + e.getMessage());
                }
            } else {
                actionPair = EntityPlayerActionPack.getActionPair(action, interval, index);
            }

            actionList.add(actionPair);
        });

        return actionList;
    }

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
}