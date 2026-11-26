package ru.footballmod.client;

/** Внешность футболиста: причёска, борода, повязка на голове, телосложение и рост (1.0 = модель в 2 блока). */
public record FootballerLook(Hair hair, boolean beard, boolean headband, float width, float height) {
    public enum Hair {
        /** Без объёмных волос (лысый или редкие волосы — только текстура). */
        NONE,
        /** Очень короткие волосы. */
        BUZZ,
        /** Обычная короткая стрижка. */
        SHORT,
        /** Короткая стрижка с чёлкой набок. */
        SHORT_SWEPT,
        /** Короткие бока и уложенный вверх верх. */
        QUIFF,
        /** Короткие бока, верх зачёсан назад. */
        SLICK,
        /** Взъерошенный ирокез до затылка. */
        MOHAWK,
        /** Длинные прямые волосы. */
        LONG,
        /** Длинные пышные кудри. */
        CURLY
    }

    public static final FootballerLook RONALDO = new FootballerLook(Hair.QUIFF, false, false, 0.96f, 0.96f);
    public static final FootballerLook RONALDO_2008 = new FootballerLook(Hair.QUIFF, false, false, 0.92f, 0.96f);
    public static final FootballerLook RONALDO_2017 = new FootballerLook(Hair.SHORT, false, false, 0.99f, 0.96f);
    public static final FootballerLook MESSI_2012 = new FootballerLook(Hair.SHORT_SWEPT, false, false, 0.9f, 0.88f);
    public static final FootballerLook REFEREE = new FootballerLook(Hair.BUZZ, false, false, 0.94f, 0.92f);
    public static final FootballerLook KEEPER = new FootballerLook(Hair.SHORT, false, false, 1.0f, 0.97f);
    public static final FootballerLook COACH = new FootballerLook(Hair.SHORT_SWEPT, true, false, 0.98f, 0.92f);
    public static final FootballerLook MESSI = new FootballerLook(Hair.SHORT_SWEPT, true, false, 0.92f, 0.88f);
    public static final FootballerLook ZIDANE = new FootballerLook(Hair.NONE, false, false, 0.97f, 0.95f);
    public static final FootballerLook RONALDINHO = new FootballerLook(Hair.CURLY, false, true, 0.93f, 0.93f);
    public static final FootballerLook NEYMAR = new FootballerLook(Hair.MOHAWK, false, false, 0.89f, 0.90f);
    public static final FootballerLook MBAPPE = new FootballerLook(Hair.BUZZ, false, false, 0.93f, 0.92f);
    public static final FootballerLook MODRIC = new FootballerLook(Hair.LONG, false, true, 0.88f, 0.885f);
    public static final FootballerLook RAMOS = new FootballerLook(Hair.SLICK, true, false, 0.97f, 0.945f);
    public static final FootballerLook XAVI = new FootballerLook(Hair.SHORT_SWEPT, false, false, 0.91f, 0.875f);
    public static final FootballerLook INIESTA = new FootballerLook(Hair.NONE, false, false, 0.89f, 0.88f);
    public static final FootballerLook SUAREZ = new FootballerLook(Hair.SHORT, false, false, 0.97f, 0.935f);
}
