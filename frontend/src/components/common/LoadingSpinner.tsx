export default function LoadingSpinner() {
  return (
    <div className="flex h-full items-center justify-center">
      <div className="relative">
        <div className="h-12 w-12 rounded-full border-b-2 border-primary-600 animate-spin"></div>
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="h-8 w-8 rounded-full border-t-2 border-primary-400 animate-spin animate-reverse"></div>
        </div>
      </div>
    </div>
  )
}