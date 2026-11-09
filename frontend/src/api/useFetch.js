import { useCallback, useEffect, useState } from 'react'
import client from './client'

export default function useFetch(url, params) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const key = JSON.stringify(params || {})

  const load = useCallback(() => {
    setLoading(true)
    return client
      .get(url, { params: JSON.parse(key) })
      .then((res) => { setData(res.data); setError(null) })
      .catch((e) => setError(e))
      .finally(() => setLoading(false))
  }, [url, key])

  useEffect(() => { load() }, [load])

  return { data, loading, error, reload: load }
}
