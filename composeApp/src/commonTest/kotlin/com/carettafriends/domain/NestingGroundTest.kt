package com.carettafriends.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The rule that decides whether a photo can become a nest.
 *
 * Every point here is real: taken from the production database and from OSM around Gazipaşa. The
 * first season's records were all logged from town — 875 m inland, in Pazarcı Mahallesi among the
 * apartment blocks — because a photo carries the coordinates of wherever it was taken, and nothing
 * checked that a turtle could have been there. These cases are that check.
 */
class NestingGroundTest {

    /** A slice of the Gazipaşa coastline (OSM `natural=coastline`), west to east. */
    private val shoreline = listOf(
        listOf(
            GeoPoint(36.25196, 32.28147),
            GeoPoint(36.25246, 32.28551),
            GeoPoint(36.25264, 32.28840),
            GeoPoint(36.25290, 32.29130),
            GeoPoint(36.25400, 32.29620),
            GeoPoint(36.25650, 32.30190),
        ),
    )

    /** Bıdı Bıdı, the community's home beach — hand-seeded, so it has a centre but no outline. */
    private val seedBeach = Beach(
        id = "bidibidi",
        communityId = "gazipasa-caretta",
        name = "Bıdı Bıdı",
        city = "Gazipaşa",
        center = GeoPoint(36.2529, 32.2869),
    )

    private fun offBy(lat: Double, lng: Double) =
        metersFromNestingGround(GeoPoint(lat, lng), listOf(seedBeach), shoreline)

    @Test
    fun nest_photographed_on_the_sand_is_accepted() {
        assertEquals(0.0, offBy(36.2529, 32.2869))       // beach centre
        assertEquals(0.0, offBy(36.25270, 32.28900))     // water's edge
    }

    @Test
    fun back_of_the_beach_is_still_the_beach() {
        // A loggerhead digs above the high-water line and volunteers photograph from the dune path.
        assertEquals(0.0, offBy(36.25450, 32.28840))     // ~200 m up from the water
    }

    @Test
    fun photo_taken_in_town_is_refused() {
        // Pazarcı Mahallesi — where GZP-21, 22 and 23 were logged from.
        val off = offBy(36.2640845, 32.2899135)
        assertNotNull(off)
        assertTrue(off > SHORE_TOLERANCE_M, "expected a refusal, got ${off}m")
    }

    @Test
    fun simulator_default_location_is_refused() {
        val off = offBy(37.3349, -122.0090)              // Cupertino
        assertNotNull(off)
        assertTrue(off > 1_000_000, "expected half a world away, got ${off}m")
    }

    @Test
    fun without_geometry_nothing_is_refused() {
        // A volunteer on a real beach must never be blocked because our cache is empty — an unknown
        // answer means "let it through", not "no".
        assertNull(metersFromNestingGround(GeoPoint(36.2640845, 32.2899135), emptyList(), emptyList()))
    }

    @Test
    fun a_beach_outline_alone_is_enough_when_the_coast_is_unknown() {
        // Muz Deniz Plajı's OSM sand ring, no coastline cached.
        val sand = Beach(
            id = "osm-way-206579045",
            communityId = "gazipasa-caretta",
            name = "Muz Deniz Plajı",
            city = "Gazipaşa",
            center = GeoPoint(36.25309, 32.28674),
            polygon = listOf(
                GeoPoint(36.25250, 32.28530),
                GeoPoint(36.25255, 32.28820),
                GeoPoint(36.25330, 32.28825),
                GeoPoint(36.25325, 32.28535),
            ),
        )
        assertEquals(0.0, metersFromNestingGround(GeoPoint(36.25290, 32.28700), listOf(sand), emptyList()))
        val inland = metersFromNestingGround(GeoPoint(36.2640845, 32.2899135), listOf(sand), emptyList())
        assertNotNull(inland)
        assertTrue(inland > SHORE_TOLERANCE_M)
    }
}
