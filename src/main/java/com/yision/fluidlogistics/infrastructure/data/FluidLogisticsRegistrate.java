package com.yision.fluidlogistics.infrastructure.data;

import java.util.List;

import com.simibubi.create.api.registrate.CreateRegistrateRegistrationCallback;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.DataProviderInitializer;
import com.tterrag.registrate.providers.ProviderType;

public final class FluidLogisticsRegistrate extends CreateRegistrate {
    private final DataProviderInitializer dataGenInitializer = new LanglessDataProviderInitializer();

    private FluidLogisticsRegistrate(String modid) {
        super(modid);
    }

    public static FluidLogisticsRegistrate create(String modid) {
        FluidLogisticsRegistrate registrate = new FluidLogisticsRegistrate(modid);
        CreateRegistrateRegistrationCallback.provideRegistrate(registrate);
        return registrate;
    }

    @Override
    public DataProviderInitializer getDataGenInitializer() {
        return dataGenInitializer;
    }

    private static final class LanglessDataProviderInitializer extends DataProviderInitializer {
        @Override
        protected List<Sorted> getSortedProviders() {
            return super.getSortedProviders().stream()
                .filter(provider -> provider.type() != ProviderType.LANG)
                .toList();
        }
    }
}
