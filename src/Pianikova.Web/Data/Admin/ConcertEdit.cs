namespace Pianikova.Web.Data;

// Mutable edit model used only by the admin schedule editor. Program/artists are kept
// structurally the same length across locales (unlike the raw per-locale JSON arrays)
// so the two languages can never silently drift out of sync through the UI.
public sealed class ConcertEdit
{
    public string Id { get; set; } = "";
    public DateTime StartsAtLocal { get; set; } = DateTime.Today.AddHours(19);
    public string Timezone { get; set; } = "Europe/Moscow";
    public string Format { get; set; } = "solo";
    public string TicketUrl { get; set; } = "";
    public string Status { get; set; } = "announced";
    public string SourceUrl { get; set; } = "";
    public DateOnly VerifiedAt { get; set; } = DateOnly.FromDateTime(DateTime.UtcNow);

    public string CityEn { get; set; } = "";
    public string CityRu { get; set; } = "";
    public string CountryEn { get; set; } = "";
    public string CountryRu { get; set; } = "";
    public string VenueEn { get; set; } = "";
    public string VenueRu { get; set; } = "";
    public string SeriesEn { get; set; } = "";
    public string SeriesRu { get; set; } = "";

    public List<ProgramWorkEdit> Program { get; } = [];
    public List<ConcertArtistEdit> Artists { get; } = [];

    internal static ConcertEdit FromDocuments(ConcertDocument en, ConcertDocument ru)
    {
        var edit = new ConcertEdit
        {
            Id = en.Id,
            StartsAtLocal = en.StartsAt.DateTime,
            Timezone = en.Timezone,
            Format = en.Format,
            TicketUrl = en.TicketUrl,
            Status = en.Status,
            SourceUrl = en.SourceUrl,
            VerifiedAt = en.VerifiedAt,
            CityEn = en.City,
            CityRu = ru.City,
            CountryEn = en.Country,
            CountryRu = ru.Country,
            VenueEn = en.Venue,
            VenueRu = ru.Venue,
            SeriesEn = en.Series,
            SeriesRu = ru.Series,
        };

        var ruProgram = ru.Program;
        for (var i = 0; i < en.Program.Count; i++)
        {
            var ruWork = i < ruProgram.Count ? ruProgram[i] : new ProgramWorkDocument(en.Program[i].Composer, "");
            edit.Program.Add(new ProgramWorkEdit
            {
                ComposerEn = en.Program[i].Composer,
                ComposerRu = ruWork.Composer,
                WorkEn = en.Program[i].Work,
                WorkRu = ruWork.Work,
            });
        }

        var ruArtists = ru.Artists;
        for (var i = 0; i < en.Artists.Count; i++)
        {
            var ruArtist = i < ruArtists.Count ? ruArtists[i] : new ConcertArtistDocument(en.Artists[i].Role, "");
            edit.Artists.Add(new ConcertArtistEdit
            {
                RoleEn = en.Artists[i].Role,
                RoleRu = ruArtist.Role,
                NameEn = en.Artists[i].Name,
                NameRu = ruArtist.Name,
            });
        }

        return edit;
    }

    internal ConcertDocument ToDocument(bool english)
    {
        var offset = SafeOffset(Timezone, StartsAtLocal);
        return new ConcertDocument(
            Id.Trim(),
            new DateTimeOffset(DateTime.SpecifyKind(StartsAtLocal, DateTimeKind.Unspecified), offset),
            Timezone.Trim(),
            english ? CityEn.Trim() : CityRu.Trim(),
            english ? CountryEn.Trim() : CountryRu.Trim(),
            english ? VenueEn.Trim() : VenueRu.Trim(),
            english ? SeriesEn.Trim() : SeriesRu.Trim(),
            Format,
            Program.Select(x => new ProgramWorkDocument(
                english ? x.ComposerEn.Trim() : x.ComposerRu.Trim(),
                english ? x.WorkEn.Trim() : x.WorkRu.Trim())).ToArray(),
            Artists.Select(x => new ConcertArtistDocument(
                english ? x.RoleEn.Trim() : x.RoleRu.Trim(),
                english ? x.NameEn.Trim() : x.NameRu.Trim())).ToArray(),
            TicketUrl.Trim(),
            Status,
            SourceUrl.Trim(),
            VerifiedAt);
    }

    private static TimeSpan SafeOffset(string timezone, DateTime local)
    {
        try
        {
            return TimeZoneInfo.FindSystemTimeZoneById(timezone).GetUtcOffset(DateTime.SpecifyKind(local, DateTimeKind.Unspecified));
        }
        catch (TimeZoneNotFoundException)
        {
            return TimeSpan.Zero;
        }
    }
}

public sealed class ProgramWorkEdit
{
    public string ComposerEn { get; set; } = "";
    public string ComposerRu { get; set; } = "";
    public string WorkEn { get; set; } = "";
    public string WorkRu { get; set; } = "";
}

public sealed class ConcertArtistEdit
{
    public string RoleEn { get; set; } = "";
    public string RoleRu { get; set; } = "";
    public string NameEn { get; set; } = "";
    public string NameRu { get; set; } = "";
}
