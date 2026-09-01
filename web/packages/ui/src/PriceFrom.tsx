import type { Locale } from '@travel/i18n';
import { t } from '@travel/i18n';
import { formatMoney, type Money } from './format';

interface Props {
  price: Money;
  locale: Locale;
}

/**
 * Giá "từ" kèm disclaimer.
 *
 * **Chữ "từ" và disclaimer là yêu cầu pháp lý, không phải lựa chọn thiết kế.**
 * Chúng nằm sẵn trong component này để không ai hiện được số trần — muốn hiện
 * giá thì phải đi qua đây.
 */
export function PriceFrom({ price, locale }: Props) {
  return (
    <p className="price-from">
      <span className="price-from__label">{t(locale, 'price.from')}</span>{' '}
      <span className="price-from__amount">{formatMoney(price, locale)}</span>
      <small className="price-from__disclaimer">{t(locale, 'price.disclaimer')}</small>
    </p>
  );
}
