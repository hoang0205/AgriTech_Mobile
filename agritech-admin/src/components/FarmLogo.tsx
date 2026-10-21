type FarmLogoProps = {
  size?: number;
};

export default function FarmLogo({ size = 32 }: FarmLogoProps) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      width={size}
      height={size}
      fill="none"
      stroke="currentColor"
      strokeWidth="1.104"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M17,12V22H4V18.613V12" />
      <path d="M17,22H22.5V4C22.5,2.895 21.605,2 20.5,2H19C17.895,2 17,2.895 17,4V9" />
      <path d="M13.9,21.612L7.1,16.087" />
      <path d="M14,22V16H7V22" />
      <path d="M17,7H22.5" />
      <path d="M17,16H22.5" />
      <path d="M17,19H22.5" />
      <path d="M2,13L10.5,7L19,13" />
      <path d="M9,11.938L10.5,11L12,11.938" />
      <path d="M2,6L3.5,7.063L5,6" />
      <path d="M6,2L7.5,3.063L9,2" />
    </svg>
  );
}