const styles = {
  P1: 'bg-red-100 text-red-700',
  P2: 'bg-orange-100 text-orange-700',
  P3: 'bg-yellow-100 text-yellow-700',
  P4: 'bg-green-100 text-green-700',
}
const labels = { P1: 'P1 Critical', P2: 'P2 High', P3: 'P3 Medium', P4: 'P4 Low' }

export default function PriorityBadge({ priority }) {
  return (
    <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${styles[priority]}`}>
      {labels[priority]}
    </span>
  )
}
