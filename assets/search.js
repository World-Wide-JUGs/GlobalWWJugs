document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('jug-search');
    const searchResults = document.getElementById('search-results');
    const jugs = Array.from(document.querySelectorAll('.region-jug'));
    const countries = Array.from(document.querySelectorAll('.country-section'));
    const regions = Array.from(document.querySelectorAll('.region-section'));
    const regionCards = Array.from(document.querySelectorAll('[data-region-card]'));
    const countryCards = Array.from(document.querySelectorAll('[data-country-card]'));
    let activeRegionId = null;
    let activeCountryId = null;

    if (!searchInput) return;

    function setVisible(element, visible) {
        element.hidden = !visible;
        element.setAttribute('aria-hidden', String(!visible));
    }

    function setCardState(card, visible, active, searching) {
        if (!card) return;
        setVisible(card, visible);
        card.classList.toggle('is-active', active);
        card.classList.toggle('is-search-match', searching && visible);
        if (active) card.setAttribute('aria-current', 'true');
        else card.removeAttribute('aria-current');
    }

    function regionForCountry(countryId) {
        const country = document.getElementById(countryId);
        const region = country && country.closest('.region-section');
        return region ? region.id : null;
    }

    function applySearch(term) {
        let visibleCount = 0;
        jugs.forEach(function(jug) {
            const visible = jug.textContent.toLowerCase().includes(term);
            setVisible(jug, visible);
            if (visible) visibleCount++;
        });

        countries.forEach(function(country) {
            const visible = Array.from(country.querySelectorAll('.region-jug')).some(function(jug) {
                return !jug.hidden;
            });
            setVisible(country, visible);
            setCardState(document.querySelector(`[data-country-card="${country.id}"]`), visible, visible, true);
        });

        regions.forEach(function(region) {
            const visible = Array.from(region.querySelectorAll('.country-section')).some(function(country) {
                return !country.hidden;
            });
            setVisible(region, visible);
            setCardState(document.querySelector(`[data-region-card="${region.id}"]`), visible, visible, true);
        });

        if (searchResults) {
            searchResults.textContent = `Found ${visibleCount} JUG${visibleCount === 1 ? '' : 's'} matching "${searchInput.value.trim()}"`;
        }
    }

    function applyNavigation() {
        const term = searchInput.value.toLowerCase().trim();
        if (term !== '') {
            applySearch(term);
            return;
        }

        const selectedRegionId = activeCountryId ? regionForCountry(activeCountryId) : activeRegionId;
        const selectedCountryId = activeCountryId;
        jugs.forEach(function(jug) {
            const country = jug.closest('.country-section');
            setVisible(jug, !selectedCountryId || country.id === selectedCountryId);
        });

        countries.forEach(function(country) {
            const inSelectedRegion = !selectedRegionId || country.closest('.region-section').id === selectedRegionId;
            const visible = inSelectedRegion && (!selectedCountryId || country.id === selectedCountryId);
            setVisible(country, visible);
            setCardState(document.querySelector(`[data-country-card="${country.id}"]`), visible, selectedCountryId === country.id, false);
        });

        regions.forEach(function(region) {
            const visible = !selectedRegionId || region.id === selectedRegionId;
            setVisible(region, visible);
            setCardState(document.querySelector(`[data-region-card="${region.id}"]`), visible, region.id === selectedRegionId, false);
        });

        if (searchResults) {
            searchResults.textContent = selectedCountryId || selectedRegionId
                ? 'Showing the selected directory section'
                : `Showing all ${jugs.length} JUGs`;
        }
    }

    function activateRegion(regionId) {
        activeRegionId = regionId;
        activeCountryId = null;
        searchInput.value = '';
        applyNavigation();
    }

    function activateCountry(countryId) {
        activeCountryId = countryId;
        activeRegionId = regionForCountry(countryId);
        searchInput.value = '';
        applyNavigation();
    }

    regionCards.forEach(function(card) {
        card.addEventListener('click', function() {
            activateRegion(card.dataset.regionCard);
        });
    });

    countryCards.forEach(function(card) {
        card.addEventListener('click', function() {
            activateCountry(card.dataset.countryCard);
        });
    });

    document.querySelectorAll('.region-backlink a').forEach(function(link) {
        link.addEventListener('click', function() {
            activeRegionId = null;
            activeCountryId = null;
            searchInput.value = '';
            applyNavigation();
        });
    });

    searchInput.addEventListener('input', function() {
        activeRegionId = null;
        activeCountryId = null;
        const term = searchInput.value.toLowerCase().trim();
        if (term === '') applyNavigation();
        else applySearch(term);
    });

    if (jugs.length === 0) {
        const legacyList = document.getElementById('jug-list');
        if (!legacyList) return;
        const items = Array.from(legacyList.children);
        items.forEach(function(item) { setVisible(item, false); });
        searchInput.addEventListener('input', function() {
            const term = searchInput.value.toLowerCase().trim();
            let visibleCount = 0;
            items.forEach(function(item) {
                const visible = term !== '' && item.textContent.toLowerCase().includes(term);
                setVisible(item, visible);
                if (visible) visibleCount++;
            });
            if (searchResults) {
                searchResults.textContent = term === ''
                    ? 'Type to search JUGs'
                    : `Found ${visibleCount} JUG${visibleCount === 1 ? '' : 's'} matching "${searchInput.value.trim()}"`;
            }
        });
        return;
    }

    applyNavigation();
});
