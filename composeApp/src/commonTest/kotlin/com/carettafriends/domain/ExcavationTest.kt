package com.carettafriends.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The excavation arithmetic — the numbers that end up in an official record.
 *
 * The formulas follow Miller (1999), the standard every nest programme reports against:
 *
 *   clutch    = empty shells (>50% intact) + unhatched + pipped
 *   hatching  = shells / clutch
 *   emergence = (shells − hatchlings still in the chamber) / clutch
 *
 * What the app adds on top is "hatchlings reached the sea", which is not Miller's — it counts the
 * ones that got out plus the ones we dug out alive. That number feeds the volunteer's profile, the
 * beach totals and the community ranking, so it has to stay inside the bounds physics allows: a
 * nest cannot send more hatchlings to the sea than it had eggs that hatched.
 */
class ExcavationTest {

    /** A textbook clutch, everything consistent: 3 stuck in the chamber, all 3 dug out alive. */
    @Test
    fun a_consistent_record_matches_the_protocol() {
        val e = Excavation(shells = 94, unhatched = 6, pipped = 2, inNest = 3, helpedOut = 3)
        assertEquals(102, e.eggsTotal)
        assertEquals(92, e.hatchSuccessPct)            // 94/102 = 92.2%
        assertEquals(89, e.emergenceSuccessPct)        // (94−3)/102 = 89.2%
        assertEquals(94, e.hatchlingsToSea)            // 91 left alone + 3 released
    }

    /** The way a volunteer actually fills it in: "I rescued 3" — without also calling them stuck. */
    @Test
    fun rescued_hatchlings_are_not_invented_out_of_thin_air() {
        val e = Excavation(shells = 94, unhatched = 6, pipped = 2, inNest = 0, helpedOut = 3)
        assertTrue(
            e.hatchlingsToSea <= e.shells,
            "94 eggs hatched but ${e.hatchlingsToSea} hatchlings reached the sea",
        )
    }

    /** A nest that produced nothing cannot still send hatchlings anywhere. */
    @Test
    fun a_failed_nest_sends_nobody_to_the_sea() {
        val e = Excavation(shells = 0, unhatched = 100, pipped = 0, inNest = 0, helpedOut = 5)
        assertEquals(0, e.hatchlingsToSea)
        assertEquals(0, e.hatchSuccessPct)
    }

    /** More stuck than hatched is a miscount; it must not turn into a negative or a phantom. */
    @Test
    fun more_stuck_than_hatched_stays_within_bounds() {
        val e = Excavation(shells = 10, unhatched = 5, pipped = 0, inNest = 20, helpedOut = 0)
        assertEquals(0, e.emergenceSuccessPct)
        assertEquals(0, e.hatchlingsToSea)
    }

    /** Rescuing more than were stuck is also a miscount — and still cannot exceed what hatched. */
    @Test
    fun rescuing_more_than_were_stuck_cannot_beat_the_clutch() {
        val e = Excavation(shells = 40, unhatched = 10, pipped = 0, inNest = 2, helpedOut = 9)
        assertTrue(e.hatchlingsToSea <= e.shells, "got ${e.hatchlingsToSea} from ${e.shells} shells")
    }

    /**
     * Percentages are rounded, not truncated.
     *
     * Truncation is what makes a volunteer distrust the screen: they count 2 of 3 by hand, get 67%,
     * and the app says 66%. Over a season it also biases every reported success rate downwards.
     */
    @Test
    fun percentages_round_the_way_a_person_would() {
        assertEquals(67, Excavation(shells = 2, unhatched = 1).hatchSuccessPct)          // 66.67%
        assertEquals(99, Excavation(shells = 199, unhatched = 1).hatchSuccessPct)        // 99.5%
        assertEquals(100, Excavation(shells = 100, unhatched = 0).hatchSuccessPct)
        assertEquals(
            88,
            Excavation(shells = 94, unhatched = 6, pipped = 2, inNest = 4).emergenceSuccessPct,
        )                                                                                // 88.24%
    }

    /** An untouched nest has no percentages to show — not 0%, which reads as "all eggs died". */
    @Test
    fun an_empty_record_has_no_percentages() {
        val e = Excavation()
        assertEquals(0, e.eggsTotal)
        assertEquals(null, e.hatchSuccessPct)
        assertEquals(null, e.emergenceSuccessPct)
        assertEquals(0, e.hatchlingsToSea)
    }

    /** Emergence can never exceed hatching: you cannot leave the nest without hatching first. */
    @Test
    fun emergence_never_exceeds_hatching() {
        for (stuck in 0..30) {
            val e = Excavation(shells = 30, unhatched = 8, pipped = 2, inNest = stuck, helpedOut = stuck)
            val hatch = e.hatchSuccessPct!!
            val emerged = e.emergenceSuccessPct!!
            assertTrue(emerged <= hatch, "stuck=$stuck → emergence $emerged% > hatching $hatch%")
            assertTrue(e.hatchlingsToSea <= e.shells, "stuck=$stuck → ${e.hatchlingsToSea} to sea")
        }
    }
}
