package com.campus.wall.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 板块标识工具类（统一规范化与校验）
 */
public final class BoardUtil {

    public static final String BOARD_CONFESSIONS = "confessions";
    public static final String BOARD_TREE_HOLE = "treehole";
    public static final String BOARD_HELP = "help";
    public static final String BOARD_MARKET = "market";
    public static final String BOARD_LOST_FOUND = "lost-found";

    private static final Set<String> VALID_BOARDS = Set.of(
            BOARD_CONFESSIONS,
            BOARD_TREE_HOLE,
            BOARD_HELP,
            BOARD_MARKET,
            BOARD_LOST_FOUND
    );

    private BoardUtil() {
    }

    /**
     * 规范化单个板块标识，返回规范值或 null（无效/空）
     */
    public static String normalizeBoardKey(String board) {
        if (board == null) {
            return null;
        }
        String value = board.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return null;
        }

        switch (value) {
            case "confession":
            case "confessions":
                return BOARD_CONFESSIONS;
            case "tree-hole":
            case "tree_hole":
            case "tree hole":
            case "treehole":
                return BOARD_TREE_HOLE;
            case "help":
            case "qa":
            case "qna":
                return BOARD_HELP;
            case "market":
            case "flea":
            case "flea-market":
            case "flea_market":
                return BOARD_MARKET;
            case "lostfound":
            case "lost-found":
            case "lost_found":
            case "lost found":
                return BOARD_LOST_FOUND;
            default:
                break;
        }

        return VALID_BOARDS.contains(value) ? value : null;
    }

    /**
     * 规范化板块列表，保持输入顺序并去重
     */
    public static List<String> normalizeBoardKeys(List<String> boards) {
        if (boards == null || boards.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String board : boards) {
            String key = normalizeBoardKey(board);
            if (key != null) {
                normalized.add(key);
            }
        }
        return new ArrayList<>(normalized);
    }

    public static boolean isValidBoard(String board) {
        return normalizeBoardKey(board) != null;
    }
}
