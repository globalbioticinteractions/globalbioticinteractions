package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.TaxonUtil;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class StudyImporterForBatPlantTest {

    @Test
    public void parseSource() throws StudyImporterException, IOException {
        String someSource = "\"745\": \"{\\\"id\\\":745,\\\"parent\\\":968,\\\"children\\\":[],\\\"sourceType\\\":{\\\"id\\\":4,\\\"displayName\\\":\\\"Citation\\\"},\\\"tags\\\":[]," +
                "\\\"interactions\\\":[3475,3476,3477,3478,3479,3480,3481,3482,3483,3484,3485,3486,3487,3488,3489,3490,3491,3492,3493,3494,34" +
                "95,3496,3497,3498,3499,3500,3501,3502,3503,3504,3505,3506,3507,3508,3509,3510,3511,3512,3513,3514,3515,3516,3517,3518,35" +
                "19,3520,3521,3522,3523,3524,3525,3526,3527,3528,3529,3530,3531,3532,3533,3534,3535,3536,3537,3538,3539,3540,3541,3542,35" +
                "43,3544,3545,3546,3547,3548,3549,3550,3551,3552,3553,3554,3555,3556,3557,3558,3559,3560,3561,3562,3563,3564,3565,3566,35" +
                "67,3568,3569,3570,3571,3572,3573,3574,3575,3576,3577,3578,3579,3580,3581,3582,3583,3584,3585,3586,3587,3588,3589,3590,35" +
                "91,3592,3593,3594,3595,3596,3597,3598,3599,3600,3601,3602,3603,3604,3605,3606,3607,3608,3609,3610,3611,3612,3613,3614,36" +
                "15,3616,3617,3618,3619,3620,3621,3622,3623,3624,3625,3626,3627,3628,3629,3630,3631,3632,3633,3634,3635,3636,3637,3638,36" +
                "39,3640,3641,3642,3643,3644,3645,3646,3647,3648,3649,3650,3651,3652,3653,3654,3655,3656,3657,3658,3659,3660,3661,3662,36" +
                "63,3664],\\\"citation\\\":40,\\\"publication\\\":347,\\\"contributors\\\":[],\\\"contributions\\\":[],\\\"displayName\\\":\\\"Impacts des pert" +
                "urbations d'origine anthropique sur les peuplements de chauves-souris en Guyane Fran\\\\u00e7aise(citation)\\\",\\\"name\\\":\\\"I" +
                "mpacts des perturbations d'origine anthropique sur les peuplements de chauves-souris en Guyane Fran\\\\u00e7aise\\\",\\\"descr" +
                "iption\\\":\\\"Delaval, M. 2004. Impacts des perturbations d'origine anthropique sur les peuplements de chauves-souris en Gu" +
                "yane Fran\\\\u00e7aise. Ph.D. Dissertation. University of Paris VI, Paris, France.\\\",\\\"year\\\":\\\"2004\\\",\\\"doi\\\":\\\" \\\",\\\"isD" +
                "irect\\\":true,\\\"serverUpdatedAt\\\":\\\"2020-02-22T02:04:15-06:00\\\"}\"";

        String someOtherSource = "\"955\": \"{\\\"id\\\":955,\\\"parent\\\":65,\\\"children\\\":[],\\\"sourceType\\\":{\\\"id\\\":4,\\\"displayName\\\":\\\"Citation\\\"},\\\"tags\\\":[],\\\"interactions\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120,121,122,123,124,125,126,127,128,129,130,131,2474,2475,2476,2477,2478,2479,2480,2481,2482,2483,2484,2485,2486,2487,2488,2489,2490,2491,2492,2493,2494,2495,2496,2497,2498,2499,2500,2501,2502,2503,2504,2505,2506,2507,2508,2509,2510,2511,2512,2513,2514,2515,2516,2517,2518,2519,2520,2521,2522,2523,2524,2525,2526,2527,2528,2529,2530,2531,2532,2533,2534,2535,2536,2537,2538,2539,2540,2541,2542,2543,2544,2545,2546,2547,2548,2549,2550,2551,2552,2553,2554,2555,2556,2557,2558,2559,2560,2561,2562,2563,2564,2565,2566,2567,2568,2569,2570,2571,2572,2573,2574,2575,2576,2577,2578,2579,2580,2581,2582,2583,2584,2585,2586,2587,2588,2589,2590,2591,2592,2593,2594,2595,2596,2597,2598,2599,2600,2601,2602,2603,2604,2605,2606,2607,2608,2609,2610,2611,2612,2613,2614,2615,2616,2617,2618,2619,2620,2621,2622,2623,2624,2625,2626,2627,2628,2629,2630,2631,2632,2633,2634,2635,2636,2637,2638,2639,2640,2641,2642,2643,2644,2645,2646,2647,2648,2649,2650,2651,2652,2653,2654,2655,2656,2657,2658,2659,2660,2661,2662,2663,2664,2665,2666,2667,2668,2669,2670,2671,2672,2673,2674,2675,2676,2677,2678,2679,2680,2681,2682,2683,2684,2685,2686,2687,2688,2689,2690,2691,2692,2693,2694,2695,2696,2697,2698,2699,2700,2701,2702,2703,2704,2705,2706,2707,2708,2709,2710,2711,2712,2713,2714,2715,2716,2717,2718,2719,2720,2721,2722,2723,2724,2725,2726,2727,2728,2729,2730,2731,2732,2733,2734,2735,2736,2737,2738,2739,2740,2741,2742,2743,2744,2745,2746,2747,2748,2749,2750,2751,2752,2753,2754,2755,2756,2757,2758,2759,2760,2761,2762,2763,2764,2765,2766,2767,2768,2769,2770,2771,2772,2773,2774,2775,2776,2777,2778,2779,2780,2781,2782,2783,2784,2785,2786,2787,2788,2789,2790,2791,2792,2793,2794,2795,2796,2797,2798,2799,2800,2801,2802,2803,2804,2805,2806,2807,2808,2809,2810,2811,2812,2813,2814,2815,2816,2817,2818,2819,2820,2821,2822,2823,2824,2825,2826,2827,2828,2829,2830,2831,2832,2833,2834,2835,2836,2837,2838,2839,2840,2841,2842,2843,2844,2845,2846,2847,2848,2849,2850,2851,2852,2853,2854,2855,2856,2857,2858,2859,2860,2861,2862,2863,2864,2865,2866,2867,2868,2869,2870,2871,2872,2873,2874,2875,2876,2877,2878,2879,2880,2881,2882,2883,2884,2885,2886,2887,2888,2889,2890,2891,2892,2893,2894,2895,2896,2897,2898,2899,2900,2901,2902,2903,2904,2905,2906,2907,2908,2909,2910,2911,2912,2913,2914,2915,2916,2917,2918,2919,2920,2921,2922,2923,2924,2925,2926,2927,2928,2929,2930,2931,2932,2933,2934,2935,2936,2937,2938,2939,2940,2941,2942,2943,2944,2945,2946,2947,2948,2949,2950,2951,2952,2953,2954,2955,2956,2957,2958,2959,2960,2961,2962,2963,2964,2965,2966,2967,2968,2969,2970,2971,2972,2973,2974,2975,2976,2977,2978,2979,2980,2981,2982,2983,2984,2985,2986,2987,2988,2989,2990,2991,2992,2993,2994,2995,2996,2997,2998,2999,3000],\\\"citation\\\":257,\\\"authors\\\":{\\\"1\\\":187},\\\"contributors\\\":{\\\"187\\\":{\\\"contribId\\\":1043,\\\"isEditor\\\":false,\\\"ord\\\":1}},\\\"contributions\\\":[],\\\"displayName\\\":\\\"Feeding habits\\\",\\\"description\\\":\\\"Gardner, A. L. 1977. Feeding habits. In: Biology of bats of the New World family Phyllostomatidae. Part II (R. J. Baker, J. K. Jones & D. C. Carter, eds.). pp. 293-350. The Museum, Texas Tech University Press, Lubbock, USA.\\\",\\\"year\\\":\\\"1977\\\",\\\"linkDisplay\\\":\\\" \\\",\\\"isDirect\\\":true,\\\"serverUpdatedAt\\\":\\\"2018-03-08T16:45:05-06:00\\\"}\"";


        String sourceChunk = "{ \"source\": {" + someSource + "," + someOtherSource + "}}";

        Map<Integer, String> sourceCitations = new TreeMap<>();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(sourceChunk);
        if (jsonNode.has("source")) {
            JsonNode sources = jsonNode.get("source");
            Iterator<Map.Entry<String, JsonNode>> taxonEntries = sources.getFields();
            while (taxonEntries.hasNext()) {
                Map.Entry<String, JsonNode> next = taxonEntries.next();
                JsonNode sourceValue = next.getValue();
                if (sourceValue.isTextual()) {
                    JsonNode sourceNode = objectMapper.readTree(sourceValue.getTextValue());
                    String id = textValueOrNull(sourceNode, "id");
                    String description = textValueOrNull(sourceNode, "description");
                    sourceCitations.put(Integer.parseInt(id), description);
                }
            }
        }

        assertThat(sourceCitations.size(), is(2));
        assertThat(sourceCitations.get(955), is("Gardner, A. L. 1977. Feeding habits. In: Biology of bats of the New World family Phyllostomatidae. Part II (R. J. Baker, J. K. Jones & D. C. Carter, eds.). pp. 293-350. The Museum, Texas Tech University Press, Lubbock, USA."));
        assertThat(sourceCitations.get(745), is("Delaval, M. 2004. Impacts des perturbations d'origine anthropique sur les peuplements de chauves-souris en Guyane Française. Ph.D. Dissertation. University of Paris VI, Paris, France."));


    }

    @Test
    public void parseTaxon() throws StudyImporterException, IOException {
        String someTaxon = "\"974\": \"{\\\"id\\\":974,\\\"realm\\\":{\\\"id\\\":1,\\\"displayName\\\":\\\"Bat\\\",\\\"pluralName\\\":\\\"Bats\\\"},\\\"level\\\":{\\\"id\\\":7,\\\"displayName\\\":\\\"Species\\\"},\\\"parent\\\":322,\\\"children\\\":[],\\\"subjectRoles\\\":[1,2,3,4,9,256,1067,2988,2989,2990,2991,2992,2993,2994,2995,2996,2997,2998,2999,3000,4052,4053,4054,4055,4056,4057,4058,4059,4060,4061,4062,4063,4064,4065,4066,4067,4068,4069],\\\"objectRoles\\\":[],\\\"displayName\\\":\\\"Micronycteris hirsuta\\\",\\\"name\\\":\\\"Micronycteris hirsuta\\\",\\\"isRealm\\\":false,\\\"serverUpdatedAt\\\":\\\"2020-02-22T02:04:15-06:00\\\"}\"";
        String someOtherTaxon = "\"885\": \"{\\\"id\\\":885,\\\"realm\\\":{\\\"id\\\":3,\\\"displayName\\\":\\\"Arthropod\\\",\\\"pluralName\\\":\\\"Arthropods\\\"},\\\"level\\\":{\\\"id\\\":5,\\\"displayName\\\":\\\"Family\\\"},\\\"parent\\\":814,\\\"children\\\":[2022,2023,2021,2051,2018,957,2019,959,2020],\\\"subjectRoles\\\":[],\\\"objectRoles\\\":[1,128,1978,1986,2616,4049,4067,5510,6888,7629,7838,9865],\\\"displayName\\\":\\\"Family Sphingidae\\\",\\\"name\\\":\\\"Sphingidae\\\",\\\"isRealm\\\":false,\\\"serverUpdatedAt\\\":\\\"2020-02-22T02:04:15-06:00\\\"}\"";

        String taxonChunk = "{ \"taxon\": { " + someOtherTaxon + "," + someTaxon + "} }";

        Map<Integer, Taxon> taxa = new TreeMap<>();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode taxaNode = objectMapper.readTree(taxonChunk);

        if (taxaNode.has("taxon")) {
            JsonNode taxon = taxaNode.get("taxon");
            Iterator<Map.Entry<String, JsonNode>> taxonEntries = taxon.getFields();
            while (taxonEntries.hasNext()) {
                Map.Entry<String, JsonNode> next = taxonEntries.next();
                JsonNode taxonValue = next.getValue();
                if (taxonValue.isTextual()) {
                    JsonNode taxonNode = objectMapper.readTree(taxonValue.getTextValue());
                    String taxonId = textValueOrNull(taxonNode, "id");
                    String taxonParentId = textValueOrNull(taxonNode, "parent");
                    String rankName = taxonNode.get("level").get("displayName").asText();
                    String taxonName = textValueOrNull(taxonNode, "name");
                    String taxonNameId = "batplant:taxon:" + taxonId;
                    String taxonParentNameId = "batplant:taxon:" + taxonParentId;
                    TaxonImpl taxonObj = new TaxonImpl(taxonName, taxonNameId);
                    taxonObj.setPathIds(StringUtils.join(taxonParentNameId, taxonNameId, CharsetConstant.SEPARATOR));
                    taxonObj.setRank(rankName);
                    taxa.put(Integer.parseInt(taxonId), taxonObj);
                }
            }
        }

        assertThat(taxa.size(), is(2));
        assertThat(taxa.get(974).getName(), is("Micronycteris hirsuta"));
        assertThat(taxa.get(885).getName(), is("Sphingidae"));

    }

    @Test
    public void parseInteraction() throws StudyImporterException, IOException {
        String interactionJson = "{\"interaction\": {" +
                "    \"1\": \"{\\\"id\\\":1,\\\"source\\\":955,\\\"interactionType\\\":{\\\"id\\\":11,\\\"displayName\\\":\\\"Predation\\\"},\\\"location\\\":30,\\\"subje" +
                "ct\\\":974,\\\"object\\\":885,\\\"tags\\\":[{\\\"id\\\":6,\\\"displayName\\\":\\\"Secondary\\\"}],\\\"updatedBy\\\":\\\"Sarah\\\",\\\"serverUpdatedAt\\\":" +
                "\\\"2020-04-28T15:44:41-05:00\\\"}\"}}";

        List<Map<String, String>> links = new ArrayList<>();
        InteractionListener testListener = links::add;


        final ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(interactionJson);

        if (jsonNode.has("interaction")) {
            JsonNode interaction = jsonNode.get("interaction");
            if (interaction.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = interaction.getFields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    JsonNode value = entry.getValue();
                    if (value.isTextual()) {
                        JsonNode interactionNode = objectMapper.readTree(value.getTextValue());
                        JsonNode interactionType = interactionNode.get("interactionType");
                        if (interactionType != null && interactionType.isObject()) {
                            String interactionTypeId = textValueOrNull(interactionType, "id");
                            String interactionTypeName = textValueOrNull(interactionType, "displayName");
                            if (StringUtils.isNotBlank(interactionTypeId)
                                    && StringUtils.isNotBlank(interactionTypeName)) {

                                Map<String, String> interactionRecord = new TreeMap<>();

                                interactionRecord.put(StudyImporterForTSV.INTERACTION_TYPE_ID, "batplant:interactionTypeId:" + interactionTypeId);
                                interactionRecord.put(StudyImporterForTSV.INTERACTION_TYPE_NAME, interactionTypeName);

                                String sourceTaxonId = textValueOrNull(interactionNode, "subject");
                                interactionRecord.put(TaxonUtil.SOURCE_TAXON_ID, "batplant:taxon:" + sourceTaxonId);
                                String targetTaxonId = textValueOrNull(interactionNode, "object");
                                interactionRecord.put(TaxonUtil.TARGET_TAXON_ID, "batplant:taxon:" + targetTaxonId);

                                String sourceId = textValueOrNull(interactionNode, "source");
                                interactionRecord.put(StudyImporterForTSV.REFERENCE_ID, "batplant:source:" + sourceId);

                                testListener.newLink(interactionRecord);
                            }

                        }
                    }

                }
            }
        }

        assertThat(links.size(), Is.is(1));

        Map<String, String> firstLink = links.get(0);
        assertThat(firstLink.get(StudyImporterForTSV.INTERACTION_TYPE_ID), is("batplant:interactionTypeId:11"));
        assertThat(firstLink.get(StudyImporterForTSV.INTERACTION_TYPE_NAME), is("Predation"));
        assertThat(firstLink.get(StudyImporterForTSV.REFERENCE_ID), is("batplant:source:955"));
        assertThat(firstLink.get(TaxonUtil.SOURCE_TAXON_ID), is("batplant:taxon:974"));
        assertThat(firstLink.get(TaxonUtil.TARGET_TAXON_ID), is("batplant:taxon:885"));
    }

    private String textValueOrNull(JsonNode interactionType, String key) {
        String textValue = null;
        JsonNode interactionTypeId = interactionType.get(key);
        if (interactionTypeId != null) {
            textValue = interactionTypeId.asText();
        }
        return textValue;
    }


}