package edu.macalester.conceptual.puzzles.constructorchains;

import java.util.List;

import edu.macalester.conceptual.context.PuzzleContext;


// random places related to the BBC Shipping Forecast and other UK weather-related stations and
// places: https://en.wikipedia.org/wiki/Shipping_Forecast#Region_names

public class RandomWeatherPlace extends RandomWordList {
    public static final List<String> sources = List.of("Viking", "North Utsire", "South", "Forties", "Cromarty",
        "Forth",
        "Tyne", "Dogger", "Fisher", "German Bight", "Humber", "Thames", "Dover", "Wight", "Portland", "Plymouth",
        "Biscay", "Trafalgar", "Fitz Roy", "Sole", "Lundy", "Fastnet", "Irish Sea", "Shannon", "Rockall", "Malin",
        "Hebrides", "Bailey", "Fair Isle", "Faeroes", "Southeast Iceland", "Tiree Automatic", "Stornoway",
        "Lerwick",
        "Wick Automatic", "Aberdeen", "Leuchars", "Boulmer", "Bridlington", "Sandettie Light Vessel Automatic",
        "Greenwich Light Vessel Automatic", "St Catherines Point Automatic", "Jersey",
        "Channel Light Vessel Automatic",
        "Scilly Automatic", "Milford Haven", "Aberporth", "Valley", "Liverpool Crosby", "Valentia", "Ronaldsway",
        "Malin Head", "Machrihanish Automatic", "Cape Wrath", "Rattray Head", "Berwick Upon Tweed", "Whitby",
        "Gibraltar Point", "North Foreland", "Selsey Bill", "Lyme Regis", "Lands End", "St Davids Head",
        "Great Orme Head", "Isle Man", "Lough Foyle", "Mull Galloway", "Mull Kintyre", "Ardnamurchan Point",
        "Shetland Isles");

    public static String getTypeName(PuzzleContext ctx) {

        return RandomWordList.getTypeName(ctx, sources);
    }

}