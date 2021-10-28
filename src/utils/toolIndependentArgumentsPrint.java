package utils;

public class toolIndependentArgumentsPrint {

    public toolIndependentArgumentsPrint(){

        System.out.println("\n" +
                "Optional tool-independent arguments\n" +
                "-----------------------------------------\n" +
                "\n" +
                "It is important to first decide whether these rules\n" +
                "are applied when READING (-read_rules)\n" +
                "or WRITING (-write_rules; DEFAULT)\n" +
                "\n" +
                "\t-read_rules\tApply these rules when a \n" +
                "\t\t\tpoint is read.\n" +
                "\t-write_rules\tApply these rules when a \n" +
                "\t\t\tpoint is written to .las file\n" +
                "\n" +
                "______________________________\n" +
                "INCLUDING / EXCLUDING POINTS:\n" +
                "------------------------------\n" +
                "*********************************************************\n" +
                "|\t\t\t\t\t\t\t|\n" +
                "| \tThese rules are applied by first checking \t|\n" +
                "|\tif a point is excluded \tand then checking \t|\n" +
                "|\tif a point is kept. This means that, e.g.       |\n" +
                "| \targument combination \"-drop_z_above 0.5 \t|\n" +
                "|\t-keep_user_data 2\" will remove all points \t|\n" +
                "|\tabove 0.5 m and from the remaining points \t|\n" +
                "| \twill only keep the points with user data \t|\n" +
                "|\tvalue 2\t\t\t\t\t\t|\n" +
                "|\t\t\t\t\t\t\t|\n" +
                "*********************************************************\n" +
                "\n" +
                "\t-drop_z_below\t[float]  \tExclude below (<) \n" +
                "\t\t\t\t\tz threshold.\n" +
                "\t\t\t\t\t\n" +
                "\t-drop_z_above\t[float]\t \tExclude points above\n" +
                "\t\t\t\t\t(>) z threshold.\n" +
                "\t\t\t\t\t\n" +
                "\t-drop_noise\t[-]\t \tExclude noise points \n" +
                "\t\t\t\t\t(classification 7)\n" +
                "\t\t\t\t\t\n" +
                "\t-remove_buffer\t[-]\t \tExclude buffer points\n" +
                "\t\t\t\t\t(synthetic).\n" +
                "\t\t\t\t\t\n" +
                "\t-drop_syntetic\t[-]\t\tExlude synthetic\n" +
                "\t\t\t\t\tpoints.\t\t\t\t\n" +
                "\t\t\t\n" +
                "\t-keep_classification \t[int]\tInclude only points \n" +
                "\t\t\t\t\twith this class.\n" +
                "\t\t\t\t\t\n" +
                "\t-drop_classification\t[int] \tExclude points with \n" +
                "\t\t\t\t\tthis class.\n" +
                "\t\t\t\t\t\n" +
                "\t-keep_user_data \t[int]\tInclude only points \n" +
                "\t\t\t\t\twith this user_data.\n" +
                "\t\t\t\t\t\n" +
                "\t-drop_user_data\t\t[int] \tExclude points with \n" +
                "\t\t\t\t\tthis user_data.\n" +
                "\t\n" +
                "\t-keep_only\t[-]\t \tInclude single echoes.\n" +
                "\t\n" +
                "\t-drop_only\t[-]\t \tExclude single echoes.\n" +
                "\t\n" +
                "\t-keep_first\t[-]\t \tInclude only first of \n" +
                "\t\t\t\t\tmany and only echoes.\n" +
                "\t\t\t\t\t\n" +
                "\t-drop_first\t[-]\t \tExclude first of many \n" +
                "\t\t\t\t\tand only echoes.\n" +
                "\t\n" +
                "\t-keep_last\t[-]\t \tInclude only last of \n" +
                "\t\t\t\t\tmany and only echoes.\n" +
                "\t\t\t\t\t\n" +
                "\t-drop_last\t[-]\t \tExclude last of many \n" +
                "\t\t\t\t\tand only echoes.\n" +
                "\t\n" +
                "\t-keep_intermediate\t[-]\tInclude intermediate \n" +
                "\t\t\t\t\techoes.\n" +
                "\t\t\t\t\t\n" +
                "\t-drop_intermediate\t[-]\tExclude intermediate \n" +
                "\t\t\t\t\techoes.\n" +
                "\t\n" +
                "_______________\n" +
                "MODIFY POINTS:\n" +
                "---------------\n" +
                "*********************************************************\n" +
                "|\t\t\t\t\t\t\t|\n" +
                "| \tThese rules are applied to the point after \t|\n" +
                "|\tchecking both -keep and -drop arguments. \t|\n" +
                "|\tAs such, e.g. argument combination\t\t|\n" +
                "|\t\"-drop_classification 3 -set_classification 3\"\t|\n" +
                "|\twill not remove all point in the .las file.\t|\n" +
                "|\t\t\t\t\t\t\t|\n" +
                "*********************************************************\n" +
                "\n" +
                "\t-translate_x\t[float]\t\tAdd this value to x \n" +
                "\t\t\t\t\tcoordinate of each point.\n" +
                "\t\t\t\t\t\n" +
                "\t-translate_y\t[float]   \t\n" +
                "\t\t     \t   \n" +
                "\t-translate_z\t[float]          \n" +
                "\t                  \n" +
                "\t-translate_i\t[int] \t\tAdd this value to \n" +
                "\t\t\t\t\tintensity of each point. \n" +
                "\t\n" +
                "\t-set_user_data\t[byte]\t \tAdd this value to \n" +
                "\t\t\t\t\tuser data of each point.\n" +
                "\t\t\t\t\t\n" +
                "\t-set_point_source_id[ushort] \tAdd this value to point \n" +
                "\t\t\t\t\tsource id of each point.\n" +
                "\n" +
                "\t\tTO BE CONTINUED\n" +
                "\t\t\t*\n" +
                "\t\t\t*\n" +
                "\t\t\t*\n" +
                "\t\t\t*\n" +
                "\t\t\t*\n" +
                "\t\t\t*\n" +
                "\t\t\t*\n" +
                "\t\t\t*\n" +
                "\t\t\t*\n" +
                "\t\t\t*\n" +
                "\t\t\t*\n" +
                "\t\t\t*\n" +
                "\t\t\t*\n" +
                "\t\t\n" +
                "\t\n" +
                "\t\n" +
                "\t\n" +
                "\t");

    }
}
