document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('jug-search');
    const searchResults = document.getElementById('search-results');
    const jugs = Array.from(document.querySelectorAll('.region-jug'));
    const countries = Array.from(document.querySelectorAll('.country-section'));
    const countrySummaries = Array.from(document.querySelectorAll('.country-summary'));
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

        countrySummaries.forEach(function(summary) {
            setVisible(summary, summary.querySelector('[data-country-card]:not([hidden])') !== null);
        });

        regions.forEach(function(region) {
            const visible = Array.from(region.querySelectorAll('.country-section')).some(function(country) {
                return !country.hidden;
            });
            setVisible(region, visible);
            region.classList.toggle('is-active', visible);
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
            setVisible(jug, Boolean(selectedRegionId) && (!selectedCountryId || country.id === selectedCountryId));
        });

        countries.forEach(function(country) {
            const inSelectedRegion = !selectedRegionId || country.closest('.region-section').id === selectedRegionId;
            const visible = Boolean(selectedRegionId) && inSelectedRegion && (!selectedCountryId || country.id === selectedCountryId);
            setVisible(country, visible);
            setCardState(document.querySelector(`[data-country-card="${country.id}"]`), Boolean(selectedRegionId) && inSelectedRegion, selectedCountryId === country.id, false);
        });

        countrySummaries.forEach(function(summary) {
            setVisible(summary, Boolean(selectedRegionId));
        });

        regions.forEach(function(region) {
            const visible = Boolean(selectedRegionId) && region.id === selectedRegionId;
            setVisible(region, visible);
            region.classList.toggle('is-active', visible);
            const cardVisible = !selectedRegionId || region.id === selectedRegionId;
            setCardState(document.querySelector(`[data-region-card="${region.id}"]`), cardVisible, region.id === selectedRegionId, false);
        });

        if (searchResults) {
            searchResults.textContent = selectedCountryId || selectedRegionId
                ? 'Showing the selected directory section'
                : 'Select a region or search for a JUG';
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
        const firstJug = document.querySelector(`#${countryId} .region-jug:not([hidden])`);
        if (firstJug) {
            requestAnimationFrame(function() {
                firstJug.scrollIntoView({ behavior: 'smooth', block: 'start' });
            });
        }
    }

    if (jugs.length === 0) {
        const legacyList = document.getElementById('jug-list');
        if (!legacyList) return;
        const items = Array.from(legacyList.children);
        const showAll = legacyList.dataset.showAll === 'true';
        items.forEach(function(item) { setVisible(item, showAll); });
        searchInput.addEventListener('input', function() {
            const term = searchInput.value.toLowerCase().trim();
            let visibleCount = 0;
            items.forEach(function(item) {
                const visible = term === '' ? showAll : item.textContent.toLowerCase().includes(term);
                setVisible(item, visible);
                if (visible) visibleCount++;
            });
            if (searchResults) {
                searchResults.textContent = term === ''
                    ? (showAll ? `Showing all ${items.length} JUGs` : 'Type to search JUGs')
                    : `Found ${visibleCount} JUG${visibleCount === 1 ? '' : 's'} matching "${searchInput.value.trim()}"`;
            }
        });
        return;
    }

    regionCards.forEach(function(card) {
        card.addEventListener('click', function(event) {
            event.preventDefault();
            activateRegion(card.dataset.regionCard);
        });
    });

    countryCards.forEach(function(card) {
        card.addEventListener('click', function(event) {
            event.preventDefault();
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

    document.querySelectorAll('.jug-details summary a').forEach(function(link) {
        link.addEventListener('click', function(event) {
            event.stopPropagation();
        });
    });

    searchInput.addEventListener('input', function() {
        activeRegionId = null;
        activeCountryId = null;
        const term = searchInput.value.toLowerCase().trim();
        if (term === '') applyNavigation();
        else applySearch(term);
    });

    const initialTarget = window.location.hash.slice(1);
    const initialElement = initialTarget && document.getElementById(initialTarget);
    if (initialElement && initialElement.classList.contains('country-section')) {
        activateCountry(initialTarget);
    } else if (initialElement && initialElement.classList.contains('region-section')) {
        activateRegion(initialTarget);
    } else {
        if (initialTarget) {
            window.history.replaceState(null, document.title, window.location.pathname + window.location.search);
        }
        applyNavigation();
    }
});
