rootProject.name = "travel-api"

// Bốn module theo docs/10 mục 3. Ranh giới phụ thuộc kiểm bằng ArchUnit,
// bài test nằm ở project gốc vì chỉ nơi đó mới nhìn thấy cả bốn.
include("domain")
include("application")
include("infrastructure")
include("web")
