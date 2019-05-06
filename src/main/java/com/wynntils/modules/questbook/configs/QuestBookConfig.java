/*
 *  * Copyright © Wynntils - 2019.
 */

package com.wynntils.modules.questbook.configs;

import com.wynntils.core.framework.settings.annotations.Setting;
import com.wynntils.core.framework.settings.annotations.SettingsInfo;
import com.wynntils.core.framework.settings.instances.SettingsClass;

@SettingsInfo(name = "questbook", displayPath = "Quest Book")
public class QuestBookConfig extends SettingsClass {

    public static QuestBookConfig INSTANCE;

    @Setting(displayName = "Allow Wynntils' Questbook", description = "Should Wynncraft's quest book be replaced by Wynntils'?")
    public boolean allowCustomQuestbook = true;

    @Setting(displayName = "Set Quest Location to Compass", description = "Should the compass point towards given coordinates of quests?")
    public boolean compassFollowQuests = true;

    @Setting(displayName = "Search Box Requires Click", description = "Should you be required to click on the search bar before typing or be able to type in the search bar immediately after opening the quest book?")
    public boolean searchBoxClickRequired = true;

    @Setting(displayName = "Fuzzy Search", description = "Should a different search algorithm be used that allows searching for acronyms and abbreviations?")
    public boolean useFuzzySearch = true;

}
