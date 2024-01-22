package org.pragmatica.config.api;

import org.pragmatica.config.api.spi.AppConfigProvider;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.type.RecordTemplate;

public interface AppConfig {
    default <T extends Record> Result<T> load(String prefix, RecordTemplate<T> template) {
        return template.load(prefix, store());
    }

    Store store();

    static AppConfig appConfig() {
        return ProviderHolder.INSTANCE.appConfig;
    }

    static <T extends Record> Result.Mapper1<T> appConfig(String prefix1, RecordTemplate<T> template1) {
        return Result.all(appConfig().load(prefix1, template1));
    }

    static <T1 extends Record, T2 extends Record> Result.Mapper2<T1, T2> appConfig(String prefix1,
                                                                                   RecordTemplate<T1> template1,
                                                                                   String prefix2,
                                                                                   RecordTemplate<T2> template2) {
        return Result.all(appConfig().load(prefix1, template1),
                          appConfig().load(prefix2, template2));
    }

    static <T1 extends Record, T2 extends Record, T3 extends Record> Result.Mapper3<T1, T2, T3> appConfig(String prefix1,
                                                                                                          RecordTemplate<T1> template1,
                                                                                                          String prefix2,
                                                                                                          RecordTemplate<T2> template2,
                                                                                                          String prefix3,
                                                                                                          RecordTemplate<T3> template3) {
        return Result.all(appConfig().load(prefix1, template1),
                          appConfig().load(prefix2, template2),
                          appConfig().load(prefix3, template3));
    }

    static <T1 extends Record, T2 extends Record, T3 extends Record, T4 extends Record> Result.Mapper4<T1, T2, T3, T4> appConfig(String prefix1,
                                                                                                                                 RecordTemplate<T1> template1,
                                                                                                                                 String prefix2,
                                                                                                                                 RecordTemplate<T2> template2,
                                                                                                                                 String prefix3,
                                                                                                                                 RecordTemplate<T3> template3,
                                                                                                                                 String prefix4,
                                                                                                                                 RecordTemplate<T4> template4) {
        return Result.all(appConfig().load(prefix1, template1),
                          appConfig().load(prefix2, template2),
                          appConfig().load(prefix3, template3),
                          appConfig().load(prefix4, template4));
    }

    static <T1 extends Record, T2 extends Record, T3 extends Record, T4 extends Record, T5 extends Record> Result.Mapper5<T1, T2, T3, T4, T5> appConfig(String prefix1,
                                                                                                                                                        RecordTemplate<T1> template1,
                                                                                                                                                        String prefix2,
                                                                                                                                                        RecordTemplate<T2> template2,
                                                                                                                                                        String prefix3,
                                                                                                                                                        RecordTemplate<T3> template3,
                                                                                                                                                        String prefix4,
                                                                                                                                                        RecordTemplate<T4> template4,
                                                                                                                                                        String prefix5,
                                                                                                                                                        RecordTemplate<T5> template5) {
        return Result.all(appConfig().load(prefix1, template1),
                          appConfig().load(prefix2, template2),
                          appConfig().load(prefix3, template3),
                          appConfig().load(prefix4, template4),
                          appConfig().load(prefix5, template5));
    }

    // If you really need more, you can do this with code similar to the above one.

    enum ProviderHolder {
        INSTANCE;
        final AppConfig appConfig = AppConfigProvider.load();
    }
}
