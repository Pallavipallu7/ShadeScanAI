import React, { useState } from 'react';
import { getShadeColor, getToothImageSvgDataUri } from '../../utils/shadeAnalyzer';
import { Scan, Eye } from 'lucide-react';

export default function ScanImageThumbnail({ 
  imageUri, 
  shade = 'A2', 
  size = 'md', // 'sm' | 'md' | 'lg'
  scanId = '',
  onClick 
}) {
  const [hasError, setHasError] = useState(false);

  React.useEffect(() => {
    setHasError(false);
  }, [imageUri]);

  // Check if image URI is a web-renderable URL (HTTP/HTTPS or Base64 Data URL)
  const isRenderableUrl = Boolean(
    imageUri && 
    !hasError && 
    (imageUri.startsWith('data:image') || imageUri.startsWith('http://') || imageUri.startsWith('https://')) &&
    !imageUri.startsWith('file://') &&
    !imageUri.startsWith('content://')
  );

  const displayUri = isRenderableUrl ? imageUri : getToothImageSvgDataUri(shade, scanId || imageUri);

  const dimensionClass = size === 'sm' 
    ? 'w-10 h-10 rounded-xl text-[10px]' 
    : size === 'lg' 
    ? 'w-16 h-16 rounded-2xl text-xs' 
    : 'w-12 h-12 rounded-xl text-xs';

  return (
    <div 
      onClick={onClick}
      className={`relative group cursor-pointer overflow-hidden border border-portal-border dark:border-portal-darkBorder bg-slate-900 shadow-sm shrink-0 ${dimensionClass}`}
    >
      <img
        src={displayUri}
        alt={`Tooth Scan Shade ${shade}`}
        onError={() => setHasError(true)}
        className="w-full h-full object-cover group-hover:scale-110 transition-transform"
      />
      {onClick && (
        <div className="absolute inset-0 bg-slate-900/40 opacity-0 group-hover:opacity-100 flex items-center justify-center text-white transition-opacity">
          <Eye className="w-4 h-4" />
        </div>
      )}
    </div>
  );
}
