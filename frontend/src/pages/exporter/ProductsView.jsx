import React from 'react';
import { Briefcase, Search, Plus, Eye, Edit3, Trash2, Loader2 } from 'lucide-react';

/**
 * ProductsView — Products catalog table with search, filter, and actions.
 */
export default function ProductsView({
  products, searchProduct, setSearchProduct, filterCategory, setFilterCategory,
  setErrors, setEditingProductId, setNewProdName, setNewProdHscode,
  setNewProdDesc, setNewProdPrice, setNewProdWeight, setShowAddDrawer,
  setSelectedAnalysisProduct, setActiveView, setAnalysisSubView,
  handleEditProduct, handleDeleteProduct, addToast,
}) {
  return (
    <div className="space-y-6 animate-in fade-in duration-300 relative">

      {/* Header section */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Products Catalog</h1>
          <p className="text-xs text-slate-500 mt-1 font-medium">Manage your product catalog, HS codes, and export scoring.</p>
        </div>
        <button
          onClick={() => { setErrors({}); setEditingProductId(null); setNewProdName(''); setNewProdHscode(''); setNewProdDesc(''); setNewProdPrice(''); setNewProdWeight(''); setShowAddDrawer(true); }}
          className="inline-flex items-center gap-1.5 px-4.5 py-2.5 font-bold text-xs text-white bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 shadow-md rounded-xl transition-all cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          Add Product
        </button>
      </div>

      {/* Search & Filter Bar */}
      <div className="bg-white border border-slate-200/80 rounded-xl p-4 shadow-sm flex flex-col sm:flex-row gap-4 items-center justify-between">
        <div className="relative w-full sm:max-w-xs">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            type="text"
            value={searchProduct}
            onChange={(e) => setSearchProduct(e.target.value)}
            placeholder="Search products by name or HS code..."
            className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-xl text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:border-sky-500 focus:ring-4 focus:ring-sky-500/10 transition-all"
          />
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <span className="text-xxs font-bold text-slate-400 uppercase tracking-wider">Filter by:</span>
          <select
            value={filterCategory}
            onChange={(e) => setFilterCategory(e.target.value)}
            className="px-3 py-2 border border-slate-200 bg-white rounded-xl text-xs font-semibold focus:outline-none cursor-pointer"
          >
            <option>All</option>
            <option>Spices</option>
            <option>Textiles</option>
            <option>Agri-Prod</option>
            <option>Handicraft</option>
          </select>
        </div>
      </div>

      {/* List Table */}
      <div className="bg-white border border-slate-200/80 rounded-2xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50/50 text-[10px] font-black text-slate-450 uppercase tracking-widest">
                <th className="py-4 px-5">Product Name</th>
                <th className="py-4 px-5">HS Code</th>
                <th className="py-4 px-5">Category</th>
                <th className="py-4 px-5">Price</th>
                <th className="py-4 px-5">Status</th>
                <th className="py-4 px-5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs font-semibold text-slate-700">
              {products.length > 0 ? (
                products
                  .filter(p => {
                    const matchesSearch = p.name.toLowerCase().includes(searchProduct.toLowerCase()) || (p.hscode || '').includes(searchProduct);
                    const matchesCat = filterCategory === 'All' || p.category === filterCategory;
                    return matchesSearch && matchesCat;
                  })
                  .map(p => (
                    <tr key={p.id} className="hover:bg-slate-50/40 transition-colors">
                      <td className="py-4 px-5 font-bold text-slate-900">{p.name}</td>
                      <td className="py-4 px-5 font-mono text-slate-500">{p.hscode}</td>
                      <td className="py-4 px-5 text-slate-500">{p.category}</td>
                      <td className="py-4 px-5 text-slate-805">INR {p.price}/{p.unit}</td>
                      <td className="py-4 px-5">
                        {p.status === 'Active' && (
                          <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span> Active
                          </span>
                        )}
                        {p.status === 'Draft' && (
                          <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-slate-500 bg-slate-100 px-2 py-0.5 rounded-full">
                            <span className="w-1.5 h-1.5 rounded-full bg-slate-400"></span> Draft
                          </span>
                        )}
                        {p.status === 'Analyzing' && (
                          <span className="inline-flex items-center gap-1.5 text-[10px] font-bold text-sky-600 bg-sky-50 px-2 py-0.5 rounded-full">
                            <Loader2 className="w-3 h-3 text-sky-500 animate-spin" /> Analyzing
                          </span>
                        )}
                      </td>
                      <td className="py-4 px-5 text-right space-x-1.5 whitespace-nowrap">
                        <button
                          onClick={() => { setSelectedAnalysisProduct(p.name); setActiveView('analysis'); setAnalysisSubView('select'); addToast(`Loading market index details for ${p.name}...`, 'success'); }}
                          className="p-1.5 text-slate-400 hover:text-sky-600 rounded-lg hover:bg-slate-50 cursor-pointer" title="Analyze Product"
                        >
                          <Eye className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleEditProduct(p)}
                          className="p-1.5 text-slate-400 hover:text-slate-700 rounded-lg hover:bg-slate-50 cursor-pointer" title="Edit specs"
                        >
                          <Edit3 className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleDeleteProduct(p.id, p.name)}
                          className="p-1.5 text-slate-400 hover:text-red-600 rounded-lg hover:bg-slate-50 cursor-pointer" title="Delete"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  ))
              ) : (
                <tr>
                  <td colSpan="6" className="py-12">
                    <div className="flex flex-col items-center justify-center max-w-sm mx-auto text-center">
                      <Briefcase className="w-12 h-12 text-slate-300 mb-4" />
                      <span className="block text-sm font-bold text-slate-800">No products added yet</span>
                      <span className="block text-xxs text-slate-400 mt-1 leading-normal">Add your first custom catalog specification to run automated compliance checks and profit audits.</span>
                      <button
                        onClick={() => { setErrors({}); setEditingProductId(null); setNewProdName(''); setNewProdHscode(''); setNewProdDesc(''); setNewProdPrice(''); setNewProdWeight(''); setShowAddDrawer(true); }}
                        className="mt-4 inline-flex items-center gap-1.5 px-4 py-2 text-xxs font-bold text-white bg-sky-500 rounded-xl hover:bg-sky-400"
                      >
                        <Plus className="w-3.5 h-3.5" /> Add Your First Product
                      </button>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Table footer */}
        <div className="p-4 border-t border-slate-100 flex items-center justify-between text-xxs font-bold text-slate-400">
          <span>Showing {products.length} of {products.length} products</span>
          <div className="flex items-center gap-2">
            <button className="px-3 py-1.5 border border-slate-200 rounded-lg hover:text-slate-700 disabled:opacity-40 cursor-pointer" disabled>Prev</button>
            <span className="text-slate-800">Page 1</span>
            <button className="px-3 py-1.5 border border-slate-200 rounded-lg hover:text-slate-700 disabled:opacity-40 cursor-pointer" disabled>Next</button>
          </div>
        </div>
      </div>
    </div>
  );
}
