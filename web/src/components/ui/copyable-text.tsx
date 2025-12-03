import React, { useState, useRef } from 'react'
import { Copy, Check } from 'lucide-react'
import { createPortal } from 'react-dom'

interface CopyableTextProps {
  text: string
  className?: string
}

export function CopyableText({ text, className = '' }: CopyableTextProps) {
  const [showTooltip, setShowTooltip] = useState(false)
  const [copied, setCopied] = useState(false)
  const triggerRef = useRef<HTMLDivElement>(null)
  const [coords, setCoords] = useState({ top: 0, left: 0 })

  const handleCopy = (e: React.MouseEvent) => {
    e.stopPropagation()
    if (!text) return
    navigator.clipboard.writeText(text)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  const handleMouseEnter = () => {
    if (triggerRef.current) {
      const rect = triggerRef.current.getBoundingClientRect()
      // Adjust position to keep it within viewport if needed (simple version for now)
      setCoords({
        top: rect.bottom + 5,
        left: rect.left
      })
      setShowTooltip(true)
    }
  }

  if (!text || text === '-') {
    return <span className={className}>{text}</span>
  }

  return (
    <>
      <div 
        ref={triggerRef}
        className={`truncate cursor-pointer hover:text-blue-600 transition-colors ${className}`}
        onMouseEnter={handleMouseEnter}
        onMouseLeave={() => setShowTooltip(false)}
      >
        {text}
      </div>
      
      {showTooltip && typeof document !== 'undefined' && createPortal(
        <div 
          className="fixed z-[9999] p-2 bg-gray-900 text-white text-xs rounded shadow-lg max-w-[300px] break-all animate-in fade-in zoom-in-95 duration-200"
          style={{ top: coords.top, left: coords.left }}
          onMouseEnter={() => setShowTooltip(true)}
          onMouseLeave={() => setShowTooltip(false)}
        >
          <div className="flex items-center gap-2">
            <span>{text}</span>
            <button 
              onClick={handleCopy}
              className="p-1 hover:bg-gray-700 rounded transition-colors flex-shrink-0"
              title="复制"
            >
              {copied ? <Check className="w-3 h-3 text-green-400" /> : <Copy className="w-3 h-3" />}
            </button>
          </div>
        </div>,
        document.body
      )}
    </>
  )
}
