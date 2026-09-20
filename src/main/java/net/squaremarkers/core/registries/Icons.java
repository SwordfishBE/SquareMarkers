package net.squaremarkers.core.registries;

import net.squaremarkers.core.objects.IconImageAddress;

import java.util.Set;

public class Icons {

    public static Set<IconImageAddress> ALL = Set.of(
            new IconImageAddress("/assets/squaremarkers/icons/", Keys.BEACON, "png"),
            new IconImageAddress("/assets/squaremarkers/icons/", Keys.END_GATEWAY, "png"),
            new IconImageAddress("/assets/squaremarkers/icons/", Keys.END_PORTAL, "png"),
            new IconImageAddress("/assets/squaremarkers/icons/", Keys.NETHER_PORTAL, "png"),
            new IconImageAddress("/assets/squaremarkers/icons/", Keys.SIGN, "png"),
            new IconImageAddress("/assets/squaremarkers/icons/", Keys.LIGHTNING, "png")
    );

    public static class Keys {
        public static final String BEACON = "squaremarkers_beacon";
        public static final String END_GATEWAY = "squaremarkers_end_gateway";
        public static final String END_PORTAL = "squaremarkers_end_portal";
        public static final String NETHER_PORTAL = "squaremarkers_nether_portal";
        public static final String SIGN = "squaremarkers_sign";
        public static final String LIGHTNING = "squaremarkers_lightning";
        public static final String CROSS_DIMENSION_PLAYER = "squaremarkers_cross_dimension_player";
    }

}
