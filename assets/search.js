document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('jug-search');
    const jugList = document.getElementById('jug-list');
    const pagination = document.querySelector('.pagination');
    const searchResults = document.getElementById('search-results');
    const originalItems = Array.from(jugList.children);
    
    if (!searchInput) return;
    
    function updateSearchResults(visibleCount, totalCount, searchTerm) {
        if (searchResults) {
            if (searchTerm === '') {
                searchResults.textContent = `Showing ${totalCount} JUGs`;
            } else {
                searchResults.textContent = `Found ${visibleCount} JUG${visibleCount === 1 ? '' : 's'} matching "${searchTerm}"`;
            }
        }
    }
    
    // Initial count
    updateSearchResults(originalItems.length, originalItems.length, '');
    
    searchInput.addEventListener('input', function() {
        const searchTerm = this.value.toLowerCase().trim();
        let visibleCount = 0;
        
        if (searchTerm === '') {
            // Show all items and pagination
            originalItems.forEach(item => {
                item.style.display = 'block';
                visibleCount++;
            });
            if (pagination) pagination.style.display = 'block';
        } else {
            // Hide pagination during search
            if (pagination) pagination.style.display = 'none';
            
            // Filter items
            originalItems.forEach(item => {
                const jugName = item.querySelector('h2').textContent.toLowerCase();
                const country = item.textContent.toLowerCase();
                
                if (jugName.includes(searchTerm) || country.includes(searchTerm)) {
                    item.style.display = 'block';
                    visibleCount++;
                } else {
                    item.style.display = 'none';
                }
            });
        }
        
        updateSearchResults(visibleCount, originalItems.length, searchTerm);
    });
});