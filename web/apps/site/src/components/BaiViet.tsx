import Link from 'next/link';
import Image from 'next/image';
import { t, type Locale } from '@travel/i18n';
import { formatDate } from '@travel/ui';
import type { PostSummary } from '@travel/api-client';
import { duongDanBlog } from '@/lib/routes';

/**
 * Thẻ một bài viết trong danh sách blog (R9).
 *
 * Ảnh và ngày đăng đều **có thể vắng** — `heroImage` và `publishedAt` là tuỳ
 * chọn trong hợp đồng. Bài chưa có ảnh thì thẻ ngắn lại chứ không hiện một ô
 * xám giả làm chỗ ảnh.
 */
export function TheBaiViet({ bai, locale }: { bai: PostSummary; locale: Locale }) {
  return (
    <article className="bai-the">
      {bai.heroImage !== undefined && (
        <Link href={duongDanBlog(locale, bai.slug)} aria-hidden="true" tabIndex={-1}>
          <Image
            className="bai-the__anh"
            src={bai.heroImage}
            // Ảnh minh hoạ của một thẻ đã có tiêu đề ngay bên cạnh: alt rỗng để
            // trình đọc màn hình bỏ qua thay vì đọc lại tiêu đề hai lần.
            alt=""
            width={640}
            height={360}
            unoptimized
          />
        </Link>
      )}

      <div className="bai-the__than">
        <h2 className="bai-the__ten">
          <Link href={duongDanBlog(locale, bai.slug)}>{bai.title}</Link>
        </h2>

        {bai.publishedAt !== undefined && (
          <p className="bai-the__ngay">
            <time dateTime={bai.publishedAt.toISOString()}>
              {formatDate(bai.publishedAt, locale)}
            </time>
          </p>
        )}

        <p>{bai.excerpt}</p>

        {bai.tags.length > 0 && (
          <p className="bai-the__the">
            {/* Thẻ là LIÊN KẾT LỌC, không phải nhãn trang trí: bấm vào ra đúng
                những bài mang thẻ đó, và bộ lọc nằm trong URL nên F5 giữ nguyên. */}
            {bai.tags.map((the) => (
              <Link
                key={the.slug}
                className="tf-chip"
                href={duongDanBlog(locale, undefined, new URLSearchParams({ tag: the.slug }))}
              >
                {the.name}
              </Link>
            ))}
          </p>
        )}
      </div>
    </article>
  );
}

/** Skeleton cho danh sách bài viết — skeleton, không phải vòng xoay. */
export function BaiVietSkeleton({ locale }: { locale: Locale }) {
  return (
    <div className="bai-luoi" aria-label={t(locale, 'state.loading')} aria-busy="true">
      {[0, 1, 2, 3].map((i) => (
        <article key={i} className="bai-the">
          <span className="skeleton-bar skeleton-bar--image" />
          <div className="bai-the__than">
            <span className="skeleton-bar" />
            <span className="skeleton-bar skeleton-bar--short" />
          </div>
        </article>
      ))}
    </div>
  );
}
